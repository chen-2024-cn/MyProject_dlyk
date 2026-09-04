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

import java.util.concurrent.TimeUnit;

/**
 * AI 增值能力开通状态服务实现（企业级缓存一致性设计）。
 *
 * 核心原则：数据库是付费状态唯一事实来源（Single Source of Truth），Redis 仅作加速层，
 * 任何缓存丢失/故障都必须能通过回源查询自愈（最终一致性）。
 *
 * 写路径（开通）：
 *   订单状态推进（事务内） → 事务 afterCommit 钩子才写正缓存并清除负缓存。
 *   原因：Redis 写操作不参与数据库事务，若在事务提交前写缓存，一旦事务回滚，
 *   会出现"未付款用户被判定已开通"的资损级误放行。
 *   afterCommit 写缓存失败只记日志告警、不抛异常：事实已落库，读路径回源自愈即可。
 *
 * 读路径（判定）：
 *   正缓存命中→放行 / 负缓存命中→拦截（防缓存穿透：未付费请求不再每次打库）/
 *   均未命中→订单表点查（走 idx_user_ability 索引）→ 按结果回填正/负缓存。
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

        // ② 回源：订单表点查（count + idx_user_ability 索引，替代全量拉取+内存过滤）
        boolean paid = tAiPaymentOrderMapper.countPaidByUserAndAbility(userId, ability.getKey()) > 0;

        // ③ 回填（懒加载自愈）：已付费写正缓存；未付费写短期负缓存防穿透
        try {
            if (paid) {
                redisService.setValue(grantKey, GRANTED_FLAG,
                        Constants.AI_PREMIUM_ABILITY_EXPIRE_SECONDS, TimeUnit.SECONDS);
                log.info("AI 增值能力缓存回填 | userId={}, ability={}", userId, ability.getKey());
            } else {
                redisService.setValue(denyKey, GRANTED_FLAG,
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
     * 事务提交后同步开通缓存：写正缓存 + 清除负缓存，失败仅告警不抛异常（读路径自愈兜底）。
     */
    private void syncGrantedCache(Integer userId, AiAbility ability) {
        String grantKey = buildGrantedKey(userId, ability);
        String denyKey = grantKey + Constants.REDIS_AI_PREMIUM_ABILITY_DENY_SUFFIX;
        try {
            redisService.setValue(grantKey, GRANTED_FLAG,
                    Constants.AI_PREMIUM_ABILITY_EXPIRE_SECONDS, TimeUnit.SECONDS);
            redisService.delete(denyKey);
            log.info("AI 增值能力开通 | userId={}, ability={}, 有效期={}天",
                    userId, ability.getKey(), Constants.AI_PREMIUM_ABILITY_EXPIRE_SECONDS / 86400);
        } catch (Exception e) {
            log.error("AI 增值能力开通缓存写入失败（不阻断主流程，读路径将回源自愈） | userId={}, ability={}",
                    userId, ability.getKey(), e);
        }
    }

    private String buildGrantedKey(Integer userId, AiAbility ability) {
        return Constants.REDIS_AI_PREMIUM_ABILITY_KEY + userId + ":" + ability.getKey();
    }
}
