-- =============================================================
-- DLYK AI 领航员 · 付费体系迁移脚本 V2（2026-09-07）
-- 目的：为 t_ai_payment_order 增加 expire_time（能力到期时间），
--       使数据库成为"付费有效期"的唯一事实来源，
--       修复「DB 语义永久有效 vs Redis TTL 30 天」的语义级不同步。
-- 执行前提：已按 ai_premium_schema.sql V1 建表且可能有存量订单数据。
-- =============================================================

-- 1. 增加 expire_time 字段（幂等：MySQL 8.x 不支持 IF NOT EXISTS 加列，重复执行报 1060 可忽略）
ALTER TABLE `t_ai_payment_order`
    ADD COLUMN `expire_time` DATETIME NULL COMMENT '能力到期时间(=paid_time+30天)，付费有效期唯一事实来源' AFTER `paid_time`;

-- 2. 存量数据回填：已支付订单按 paid_time + 30 天补算到期时间；
--    paid_time 为空的历史脏数据按 create_time 兜底推算
UPDATE `t_ai_payment_order`
SET `expire_time` = DATE_ADD(COALESCE(`paid_time`, `create_time`), INTERVAL 30 DAY)
WHERE `status` = 1
  AND `expire_time` IS NULL;

-- 3. 校验：确认没有"已支付但无到期时间"的残留订单（结果应为 0）
SELECT COUNT(*) AS unfilled_paid_orders
FROM `t_ai_payment_order`
WHERE `status` = 1 AND `expire_time` IS NULL;
