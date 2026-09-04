package com.cyk.service;

import com.cyk.model.TAiPaymentOrder;
import com.cyk.result.R;

import java.util.List;

/**
 * AI 增值能力支付服务（订单状态机驱动）。
 *
 * 状态流转规则（单向、幂等）：
 *   createOrder  -> 生成 0待支付 订单
 *   payOrder     -> 0 -> 1（支付成功后自动开通对应能力）
 *   cancelOrder  -> 0 -> 2
 * 非法流转一律拒绝（如 已支付->取消、重复支付均被状态机挡住）。
 */
public interface AiPaymentService {

    /**
     * 创建支付订单（幂等：同一用户对同一能力已有「待支付」订单时直接复用）。
     */
    R createOrder(Integer userId, String abilityKey);

    /**
     * 模拟支付回调：推进订单至已支付，并开通对应增值能力。
     */
    R payOrder(Integer userId, String orderNo);

    /**
     * 取消订单（仅限待支付状态）。
     */
    R cancelOrder(Integer userId, String orderNo);

    /**
     * 查询当前用户的订单列表（最近 50 条）
     */
    List<TAiPaymentOrder> listMyOrders(Integer userId);
}
