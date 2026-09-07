package com.cyk.task;

import com.cyk.constants.Constants;
import com.cyk.mapper.TAiPaymentOrderMapper;
import com.cyk.result.ai.AiAbility;
import com.cyk.service.RedisService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 付费墙缓存对账任务（企业级一致性兜底，三道防线的最后一道）。
 *
 * 解决的问题：Redis 开通标记与数据库真相比对收敛——尤其是"绕过应用直改数据库"
 *（手工 UPDATE 订单状态、DBA 数据订正、迁移脚本等）场景，这类旁路修改不会经过
 * 应用的 afterCommit 钩子，缓存侧完全无感知，只有周期对账才能发现并纠偏。
 *
 * 对账规则（以 DB 的 status=1 AND expire_time>NOW() 为唯一事实来源）：
 *   ① DB 已开通但 Redis 无正缓存          → 补写正缓存（TTL=剩余有效期）并清除负缓存；
 *   ② DB 未开通/已过期但 Redis 有正缓存    → 删除正缓存（纠偏"误放行"，资损方向优先纠正）；
 *   ③ Redis 正缓存 TTL 与 DB 剩余有效期漂移 → 重新对齐 TTL（覆盖手工改 expire_time 场景）；
 *   ④ DB 已开通但 Redis 有负缓存           → 删除负缓存（纠偏"误拦截"）。
 *
 * 实现方式：不做全表扫描，而是从 AiAbility 枚举 × 活跃用户（近 50 单的去重 user_id）
 * 笛卡尔积推导全部应存在的 key，逐一点查对账。付费墙 key 总量 = 能力数(2) × 用户数，
 * 规模可控；Redis 侧无需 SCAN（避免大 key 空间遍历阻塞）。
 *
 * 容错：整任务 try/catch，Redis 或 DB 故障时跳过本轮（下个周期重试），绝不抛出影响调度线程；
 *      仅在实际发生纠偏动作时输出 WARN 日志（便于人工回溯"谁动了数据库"），
 *      平稳运行时无日志噪音。
 */
@Slf4j
@Component
public class AiPremiumCacheReconcileTask {

    @Resource
    private RedisService redisService;

    @Resource
    private TAiPaymentOrderMapper tAiPaymentOrderMapper;

    /**
     * 周期对账（默认每 5 分钟一轮，fixedDelay 保证上轮结束后才计时，天然防止任务堆叠）。
     * 周期即"手工改库后最大不一致窗口"，按业务容忍度在 application.yml 调整。
     */
    @Scheduled(fixedDelayString = "${project.task.ai-reconcile-delay:300000}",
            zone = "Asia/Shanghai", timeUnit = java.util.concurrent.TimeUnit.MILLISECONDS,
            initialDelay = 30000)
    public void reconcile() {
        try {
            List<Integer> userIds = tAiPaymentOrderMapper.selectActiveUserIds();
            if (userIds == null || userIds.isEmpty()) {
                return; // 无任何订单的用户无需对账（负缓存由读路径 TTL 自愈）
            }
            int fixed = 0;
            for (Integer userId : userIds) {
                for (AiAbility ability : AiAbility.values()) {
                    if (!ability.isPremium()) {
                        continue;
                    }
                    if (reconcileOne(userId, ability)) {
                        fixed++;
                    }
                }
            }
            if (fixed > 0) {
                log.warn("付费墙缓存对账完成：本轮纠偏 {} 处（存在绕过应用的数据库修改或缓存漂移，请核查订单表变更来源）", fixed);
            }
        } catch (Exception e) {
            // 对账失败不影响任何业务主流程，下个周期自动重试
            log.error("付费墙缓存对账任务异常，本轮跳过 | e={}", e.getMessage(), e);
        }
    }

    /**
     * 单个 <用户, 能力> 维度的缓存收敛，返回是否实际执行了纠偏动作。
     */
    private boolean reconcileOne(Integer userId, AiAbility ability) {
        String grantKey = buildGrantedKey(userId, ability);
        String denyKey = grantKey + Constants.REDIS_AI_PREMIUM_ABILITY_DENY_SUFFIX;

        // DB 真相：已支付且未到期的最大到期时间（null = 无有效开通）
        Date maxExpire = tAiPaymentOrderMapper.selectMaxExpireTimeOfPaid(userId, ability.getKey());

        boolean grantedInRedis;
        Long redisTtl;
        try {
            grantedInRedis = Boolean.TRUE.equals(redisService.hasKey(grantKey));
            redisTtl = grantedInRedis ? redisService.getExpireSeconds(grantKey) : null;
        } catch (Exception e) {
            // Redis 故障时读路径本就有 DB 降级，对账侧仅告警跳过，不做危险操作
            log.warn("对账读 Redis 失败，跳过 | userId={}, ability={}", userId, ability.getKey(), e);
            return false;
        }

        if (maxExpire == null) {
            // 规则②：DB 无有效开通 → 正缓存必须清除（误放行是资损方向，优先纠正）
            if (grantedInRedis) {
                redisService.delete(grantKey);
                log.warn("对账纠偏[误放行]：DB 已无有效开通但 Redis 正缓存仍在，已删除 | userId={}, ability={}",
                        userId, ability.getKey());
                return true;
            }
            return false;
        }

        // DB 确认已开通：期望 TTL（秒）与允许漂移阈值（60 秒，避免每轮都重写）
        long expectTtl = Math.max(1L, (maxExpire.getTime() - System.currentTimeMillis()) / 1000);
        boolean fixed = false;

        if (!grantedInRedis) {
            // 规则①：DB 已开通但正缓存缺失 → 补写（覆盖手工把 status 改回 1、缓存被误删等场景）
            redisService.setValue(grantKey, "1", expectTtl, java.util.concurrent.TimeUnit.SECONDS);
            log.warn("对账纠偏[缓存缺失]：DB 已开通但 Redis 正缓存缺失，已补写 | userId={}, ability={}, ttl={}s",
                    userId, ability.getKey(), expectTtl);
            fixed = true;
        } else if (redisTtl != null && (redisTtl < 0 || Math.abs(redisTtl - expectTtl) > 60)) {
            // 规则③：TTL 漂移（-1 未设过期 / 与 DB 剩余有效期偏差超阈值）→ 重新对齐
            //         覆盖手工改 expire_time、旧版固定 30 天 TTL 存量等场景
            redisService.setValue(grantKey, "1", expectTtl, java.util.concurrent.TimeUnit.SECONDS);
            log.warn("对账纠偏[TTL漂移]：Redis TTL={}s 与 DB 剩余有效期={}s 不一致，已重对齐 | userId={}, ability={}",
                    redisTtl, expectTtl, userId, ability.getKey());
            fixed = true;
        }

        // 规则④：DB 已开通则负缓存不应存在（负缓存会误拦截已付费用户）
        if (Boolean.TRUE.equals(redisService.hasKey(denyKey))) {
            redisService.delete(denyKey);
            log.warn("对账纠偏[误拦截]：DB 已开通但 Redis 负缓存仍在，已删除 | userId={}, ability={}",
                    userId, ability.getKey());
            fixed = true;
        }
        return fixed;
    }

    private String buildGrantedKey(Integer userId, AiAbility ability) {
        return Constants.REDIS_AI_PREMIUM_ABILITY_KEY + userId + ":" + ability.getKey();
    }
}
