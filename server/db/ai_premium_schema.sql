-- =============================================================
-- DLYK AI 领航员 · 增值付费体系建表脚本（MySQL 8.x）
-- 执行前提：已存在 dlyk 业务库
-- 设计说明：
--   1. 金额统一使用 DECIMAL(10,2)，禁止 DOUBLE（浮点误差是金融类字段大忌）；
--   2. 状态流转：0待支付 -> 1已支付 / 2已取消，状态变更只允许单向推进；
--   3. 所有审计字段（create_time 等）由 SQL 默认值生成，代码侧不干预。
-- =============================================================

CREATE TABLE IF NOT EXISTS `t_ai_payment_order` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键，自动增长',
    `order_no`        VARCHAR(64)   NOT NULL COMMENT '支付流水号（对外唯一业务标识：AIyyyyMMddHHmmss + 随机数）',
    `user_id`         INT           NOT NULL COMMENT '下单用户ID（t_user.id）',
    `ability_key`     VARCHAR(64)   NOT NULL COMMENT '所购增值能力标识（对应 AiAbility 枚举 key，如 deep_analysis）',
    `ability_name`    VARCHAR(64)   NOT NULL COMMENT '能力名称快照（下单时冗余，避免后续能力改名影响历史账单）',
    `price`           DECIMAL(10,2) NOT NULL COMMENT '本次支付金额（元）',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付 1已支付 2已取消',
    `paid_time`       DATETIME      NULL COMMENT '实际支付完成时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `remark`          VARCHAR(255)  NULL COMMENT '备注（幂等键、取消原因等）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id_status` (`user_id`, `status`),
    KEY `idx_user_ability` (`user_id`, `ability_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AI增值能力支付订单表';
