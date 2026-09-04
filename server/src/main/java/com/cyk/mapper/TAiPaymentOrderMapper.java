package com.cyk.mapper;

import com.cyk.model.TAiPaymentOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 增值能力支付订单持久层
 */
public interface TAiPaymentOrderMapper {

    int insertSelective(TAiPaymentOrder record);

    TAiPaymentOrder selectByPrimaryKey(Long id);

    TAiPaymentOrder selectByOrderNo(@Param("orderNo") String orderNo);

    List<TAiPaymentOrder> selectByUserId(@Param("userId") Integer userId);

    /**
     * 点查：统计指定用户指定能力「已支付」的订单数量（命中唯一索引范围扫描，替代全量拉取）。
     * 返回 0/正整数，用于付费墙判定（>0 即视为已开通）。
     */
    int countPaidByUserAndAbility(@Param("userId") Integer userId,
                                  @Param("abilityKey") String abilityKey);

    /**
     * 状态机推进：仅当订单当前状态 = expectStatus 时才更新为 targetStatus（乐观锁语义）。
     * 返回影响行数：0 表示状态已被并发变更（重复支付/重复取消），调用方按幂等失败处理。
     */
    int transitStatus(@Param("orderNo") String orderNo,
                      @Param("expectStatus") int expectStatus,
                      @Param("targetStatus") int targetStatus);
}
