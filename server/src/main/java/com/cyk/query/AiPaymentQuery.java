package com.cyk.query;

import lombok.Data;

/**
 * AI 增值能力支付相关接口的统一请求体：
 * - 下单：传 abilityKey
 * - 支付/取消：传 orderNo
 */
@Data
public class AiPaymentQuery {

    /** 要购买的增值能力标识 */
    private String abilityKey;

    /** 支付/取消的目标订单流水号 */
    private String orderNo;
}
