package com.cyk.mapper;

import com.cyk.model.TAiPaymentOrder;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
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
     * 付费墙点查：取指定用户指定能力「已支付且未过期」订单的最大到期时间
     *（命中 idx_user_ability 索引；多次购买续费叠加时自然取最晚到期者）。
     * 返回 null 表示无有效开通（未支付过或已全部过期）。
     * DB 的 expire_time 是付费有效期的唯一事实来源。
     */
    Date selectMaxExpireTimeOfPaid(@Param("userId") Integer userId,
                                   @Param("abilityKey") String abilityKey);

    /**
     * 对账任务专用：近期有订单行为的去重用户ID列表（按最新下单时间倒序，最多 200 人）。
     * 用于推导付费墙缓存 key 空间，避免全表扫描；更久远用户依赖读路径回源自愈。
     */
    List<Integer> selectActiveUserIds();

    /**
     * 状态机推进：仅当订单当前状态 = expectStatus 时才更新为 targetStatus（乐观锁语义）。
     * 返回影响行数：0 表示状态已被并发变更（重复支付/重复取消），调用方按幂等失败处理。
     */
    int transitStatus(@Param("orderNo") String orderNo,
                      @Param("expectStatus") int expectStatus,
                      @Param("targetStatus") int targetStatus);
}
