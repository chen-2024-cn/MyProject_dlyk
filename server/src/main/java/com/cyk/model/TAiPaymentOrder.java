package com.cyk.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AI 增值能力支付订单表
 * t_ai_payment_order
 *
 * 状态机（单向流转，禁止回退）：
 *   0 待支付 -> 1 已支付
 *   0 待支付 -> 2 已取消
 */
@Data
public class TAiPaymentOrder implements Serializable {

    /** 待支付 */
    public static final int STATUS_PENDING = 0;
    /** 已支付 */
    public static final int STATUS_PAID = 1;
    /** 已取消 */
    public static final int STATUS_CANCELLED = 2;

    /**
     * 主键，自动增长
     */
    private Long id;

    /**
     * 支付流水号（对外唯一业务标识）
     */
    private String orderNo;

    /**
     * 下单用户ID（t_user.id）
     */
    private Integer userId;

    /**
     * 所购增值能力标识（对应 AiAbility 枚举 key）
     */
    private String abilityKey;

    /**
     * 能力名称快照（下单时冗余，避免能力改名影响历史账单）
     */
    private String abilityName;

    /**
     * 本次支付金额（元）
     */
    private BigDecimal price;

    /**
     * 支付状态：0待支付 1已支付 2已取消
     */
    private Integer status;

    /**
     * 实际支付完成时间
     */
    private Date paidTime;

    /**
     * 能力到期时间（= 支付完成时间 + 30 天；待支付/已取消订单为 NULL）。
     * 付费有效期的唯一事实来源：付费墙判定条件为 status=已支付 AND expire_time > NOW()；
     * Redis 正缓存 TTL 与本字段剩余时长动态对齐，到期后需重新下单续费激活。
     * ⚠ 绕过应用手工改单时必须同步维护本字段，否则按无有效开通处理。
     */
    private Date expireTime;

    /**
     * 下单时间
     */
    private Date createTime;

    /**
     * 备注（幂等键、取消原因等）
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
