package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.mapper.TAiPaymentOrderMapper;
import com.cyk.result.ai.AiAbility;
import com.cyk.service.AiPremiumAbilityService;
import com.cyk.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * AI 增值能力开通状态服务实现（企业级缓存一致性设计）。
 *
 * 核心原则：数据库是付费状态与付费有效期（expire_time）的唯一事实来源（Single Source of Truth），
 * Redis 仅作加速层，任何缓存丢失/故障/漂移都能通过回源查询自愈（最终一致性）。
 *
 * 一致性闭环（三道防线）：
 *   1. 写路径：订单事务 afterCommit 才写缓存（防回滚误放行）；缓存 TTL 动态对齐 DB 剩余有效期；
 *   2. 读路径：正缓存→负缓存→DB 点查（status=1 且未过期）→ SETNX 回填（防并发覆盖竞态）；
 *   3. 对账兜底：AiPremiumCacheReconcileTask 周期扫描缓存与 DB 收敛，
 *      覆盖"绕过应用直改数据库"的旁路场景（手工 UPDATE 后最长一个对账周期内自动纠偏）。
 *
 * 写路径（开通）：
 *   订单状态推进（事务内，SQL 同步写 paid_time + expire_time） → afterCommit 写正缓存并清除负缓存。
 *   原因：Redis 写操作不参与数据库事务，若在事务提交前写缓存，一旦事务回滚，
 *   会出现"未付款用户被判定已开通"的资损级误放行。
 *   afterCommit 写缓存失败只记日志告警、不抛异常：事实已落库，读路径回源自愈即可。
 *
 * 读路径（判定）：
 *   正缓存命中→放行 / 负缓存命中→拦截（防缓存穿透：未付费请求不再每次打库）/
 *   均未命中→订单表点查（走 idx_user_ability 索引，取未过期已支付订单的 MAX(expire_time)）
 *   → 按结果 SETNX 回填正/负缓存（正缓存 TTL = DB 剩余有效期，两侧到期语义完全一致）。
 *   Redis 故障时降级直查数据库，缓存层不可用不阻断主流程（可用性优先）。
 */
@Slf4j
@Service
public class AiPremiumAbilityServiceImpl implements AiPremiumAbilityService {

    /** Redis 开通标记的 value（存在即代表已开通） */
    private static final String GRANTED_FLAG = "1";

    @Resource
    private RedisService redisService;

    @Resource
    private TAiPaymentOrderMapper tAiPaymentOrderMapper;

    @Override
    public boolean isGranted(Integer userId, AiAbility ability) {
        if (ability == null) {
            return false;
        }
        // 免费能力无需付费墙，直接放行
        if (!ability.isPremium()) {
            return true;
        }
        if (userId == null) {
            return false;
        }

        String grantKey = buildGrantedKey(userId, ability);
        String denyKey = grantKey + Constants.REDIS_AI_PREMIUM_ABILITY_DENY_SUFFIX;

        // ① 缓存判定（正/负），Redis 异常时降级直查数据库，不阻断业务
        try {
            if (Boolean.TRUE.equals(redisService.hasKey(grantKey))) {
                return true;
            }
            if (Boolean.TRUE.equals(redisService.hasKey(denyKey))) {
                return false;
            }
        } catch (Exception e) {
            log.warn("付费墙读 Redis 失败，降级直查数据库 | userId={}, ability={}",
                    userId, ability.getKey(), e);
        }

        // ② 回源：订单表点查「已支付且未过期」的最大到期时间（DB expire_time 是有效期唯一事实来源；
        //    多次购买续费叠加时 MAX 自然取最晚到期者，全部过期返回 null 等价未开通）
        Date maxExpire = tAiPaymentOrderMapper.selectMaxExpireTimeOfPaid(userId, ability.getKey());
        boolean paid = maxExpire != null;

        // ③ 回填（懒加载自愈）：SETNX 条件写入，并发回填互不覆盖对方最新结果；
        //    正缓存 TTL 动态对齐 DB 剩余有效期，Redis 与 DB 到期时间天然一致
        try {
            if (paid) {
                redisService.setValueIfAbsent(grantKey, GRANTED_FLAG, calcGrantTtlSeconds(maxExpire), TimeUnit.SECONDS);
                log.info("AI 增值能力缓存回填 | userId={}, ability={}, 剩余={}秒",
                        userId, ability.getKey(), calcGrantTtlSeconds(maxExpire));
            } else {
                redisService.setValueIfAbsent(denyKey, GRANTED_FLAG,
                        Constants.AI_PREMIUM_ABILITY_DENY_EXPIRE_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("付费墙回填 Redis 失败（不影响本次判定，下次请求自愈） | userId={}, ability={}",
                    userId, ability.getKey(), e);
        }
        return paid;
    }

    @Override
    public void grantAbility(Integer userId, AiAbility ability) {
        if (userId == null || ability == null) {
            return;
        }

        // 企业级关键设计：缓存写入延迟到宿主事务"提交成功后"执行，
        // 避免事务回滚后 Redis 残留开通标记造成误放行（资损风险）。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    syncGrantedCache(userId, ability);
                }
            });
        } else {
            // 防御性分支：无事务上下文调用时直接写（事实数据已由上游保证落库）
            syncGrantedCache(userId, ability);
        }
    }

    @Override
    public int countGranted(Integer userId) {
        if (userId == null) {
            return 0;
        }
        int count = 0;
        for (AiAbility ability : AiAbility.values()) {
            if (ability.isPremium() && isGranted(userId, ability)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 事务提交后同步开通缓存：以 DB 最新 expire_time 为准写正缓存（TTL=剩余有效期），
     * 并清除负缓存。失败仅告警不抛异常（读路径回源 + 对账任务双重兜底自愈）。
     */
    private void syncGrantedCache(Integer userId, AiAbility ability) {
        String grantKey = buildGrantedKey(userId, ability);
        String denyKey = grantKey + Constants.REDIS_AI_PREMIUM_ABILITY_DENY_SUFFIX;
        try {
            // 事务已提交，此时能查到刚落库的 expire_time；缓存 TTL 与 DB 有效期严格对齐，
            // 杜绝"Redis 30天 vs DB 永久/其他时长"的语义漂移
            Date maxExpire = tAiPaymentOrderMapper.selectMaxExpireTimeOfPaid(userId, ability.getKey());
            if (maxExpire != null) {
                redisService.setValue(grantKey, GRANTED_FLAG, calcGrantTtlSeconds(maxExpire), TimeUnit.SECONDS);
                log.info("AI 增值能力开通 | userId={}, ability={}, 剩余有效期={}秒",
                        userId, ability.getKey(), calcGrantTtlSeconds(maxExpire));
            } else {
                // 理论不可达（刚支付成功必有未过期 expire_time），出现说明时钟/数据异常，仅告警
                log.error("AI 增值能力开通异常：DB 无有效到期时间，跳过缓存写入 | userId={}, ability={}",
                        userId, ability.getKey());
                return;
            }
            redisService.delete(denyKey);
        } catch (Exception e) {
            log.error("AI 增值能力开通缓存写入失败（不阻断主流程，读路径/对账任务将自愈） | userId={}, ability={}",
                    userId, ability.getKey(), e);
        }
    }

    /**
     * 依据 DB 到期时间计算正缓存应设置的 TTL（秒），最小 1 秒防止已过期的脏数据被写入缓存。
     */
    private long calcGrantTtlSeconds(Date maxExpire) {
        long remain = (maxExpire.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(1L, remain);
    }

    private String buildGrantedKey(Integer userId, AiAbility ability) {
        return Constants.REDIS_AI_PREMIUM_ABILITY_KEY + userId + ":" + ability.getKey();
    }
}
