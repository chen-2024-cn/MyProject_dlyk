package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.mapper.TAiPaymentOrderMapper;
import com.cyk.model.TAiPaymentOrder;
import com.cyk.result.CodeEnum;
import com.cyk.result.R;
import com.cyk.result.ai.AiAbility;
import com.cyk.service.AiPaymentService;
import com.cyk.service.AiPremiumAbilityService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 增值能力支付服务实现（模拟支付网关 + 订单状态机）。
 *
 * 企业级设计要点：
 * 1. 状态机单向推进由 SQL 层乐观锁保证（transitStatus WHERE status=?），天然幂等；
 * 2. 下单幂等：同一用户对同一能力已存在「待支付」订单时直接复用，不产生垃圾单；
 * 3. 归属校验：支付/取消前校验订单归属，防止横向越权操作他人订单；
 * 4. 支付成功后同一事务内开通能力，订单与开通状态强一致。
 */
@Slf4j
@Service
public class AiPaymentServiceImpl implements AiPaymentService {

    @Resource
    private TAiPaymentOrderMapper tAiPaymentOrderMapper;

    @Resource
    private AiPremiumAbilityService aiPremiumAbilityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R createOrder(Integer userId, String abilityKey) {
        AiAbility ability = AiAbility.fromKey(abilityKey);
        if (ability == null || !ability.isPremium()) {
            return R.FAIL(CodeEnum.AI_ABILITY_NOT_FOUND);
        }

        // 幂等：同一用户同一能力若已有待支付订单，直接复用
        TAiPaymentOrder pending = findPendingOrder(userId, abilityKey);
        if (pending != null) {
            log.info("AI 下单幂等复用 | userId={}, orderNo={}", userId, pending.getOrderNo());
            return R.OK(pending);
        }

        TAiPaymentOrder order = new TAiPaymentOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAbilityKey(ability.getKey());
        order.setAbilityName(ability.getName());
        order.setPrice(ability.getPrice());
        order.setStatus(TAiPaymentOrder.STATUS_PENDING);
        order.setCreateTime(new Date());
        tAiPaymentOrderMapper.insertSelective(order);
        log.info("AI 增值能力下单成功 | userId={}, ability={}, orderNo={}",
                userId, abilityKey, order.getOrderNo());
        return R.OK(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R payOrder(Integer userId, String orderNo) {
        TAiPaymentOrder order = tAiPaymentOrderMapper.selectByOrderNo(orderNo);
        // 归属校验：订单不存在或不属于当前登录人 → 统一按"找不到"处理，不泄露他人订单存在性
        if (order == null || !order.getUserId().equals(userId)) {
            return R.FAIL(CodeEnum.AI_ORDER_NOT_FOUND);
        }

        // 幂等：已支付订单直接返回成功（模拟支付网关重复回调场景）
        if (order.getStatus() == TAiPaymentOrder.STATUS_PAID) {
            return R.OK(order);
        }
        if (order.getStatus() == TAiPaymentOrder.STATUS_CANCELLED) {
            return R.FAIL(CodeEnum.AI_ORDER_STATUS_ILLEGAL);
        }

        // 状态机推进：待支付 -> 已支付（SQL 乐观锁，并发双花时只有一个请求能成功）
        int updated = tAiPaymentOrderMapper.transitStatus(orderNo,
                TAiPaymentOrder.STATUS_PENDING, TAiPaymentOrder.STATUS_PAID);
        if (updated == 0) {
            return R.FAIL(CodeEnum.AI_PAYMENT_FAILED);
        }

        // 开通对应增值能力
        AiAbility ability = AiAbility.fromKey(order.getAbilityKey());
        if (ability != null) {
            aiPremiumAbilityService.grantAbility(userId, ability);
        }
        log.info("AI 增值能力支付成功 | userId={}, orderNo={}, ability={}",
                userId, orderNo, order.getAbilityKey());

        // 返回最新状态
        return R.OK(tAiPaymentOrderMapper.selectByOrderNo(orderNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R cancelOrder(Integer userId, String orderNo) {
        TAiPaymentOrder order = tAiPaymentOrderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            return R.FAIL(CodeEnum.AI_ORDER_NOT_FOUND);
        }
        if (order.getStatus() == TAiPaymentOrder.STATUS_PAID) {
            // 已支付订单不允许取消（模拟支付不支持退款，退款走独立客服流程）
            return R.FAIL(CodeEnum.AI_ORDER_STATUS_ILLEGAL);
        }
        int updated = tAiPaymentOrderMapper.transitStatus(orderNo,
                TAiPaymentOrder.STATUS_PENDING, TAiPaymentOrder.STATUS_CANCELLED);
        if (updated == 0) {
            return R.FAIL(CodeEnum.AI_PAYMENT_FAILED);
        }
        return R.OK(tAiPaymentOrderMapper.selectByOrderNo(orderNo));
    }

    @Override
    public List<TAiPaymentOrder> listMyOrders(Integer userId) {
        return tAiPaymentOrderMapper.selectByUserId(userId);
    }

    /**
     * 生成支付流水号：前缀 + 时间戳 + 4位随机数（单机演示足够唯一，生产应接入发号器）
     */
    private String generateOrderNo() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return Constants.AI_PAYMENT_ORDER_NO_PREFIX + timestamp + random;
    }

    /**
     * 查找同一用户对同一能力的待支付订单（下单幂等依据）
     */
    private TAiPaymentOrder findPendingOrder(Integer userId, String abilityKey) {
        List<TAiPaymentOrder> orders = tAiPaymentOrderMapper.selectByUserId(userId);
        if (orders == null) {
            return null;
        }
        return orders.stream()
                .filter(o -> abilityKey.equals(o.getAbilityKey())
                        && o.getStatus() != null
                        && o.getStatus() == TAiPaymentOrder.STATUS_PENDING)
                .findFirst()
                .orElse(null);
    }
}
