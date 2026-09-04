package com.cyk.mapper;

import com.cyk.model.TTran;
import com.cyk.result.TrendPoint;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TTranMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTran record);

    int insertSelective(TTran record);

    TTran selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTran record);

    int updateByPrimaryKey(TTran record);

    int updateCustomerIdToNullByCustomerId(Integer customerId);

    BigDecimal selectBySuccessTranAmount();

    BigDecimal selectByTotalTranAmount();

    int selectByTotalTranCount();

    int selectBySuccessTranCount();

    List<TTran> selectTranByPage(@Param("customerId") Integer customerId, @Param("money") BigDecimal money);

    TTran selectById(Integer id);

    String selectMaxTranNoByDate(@Param("datePrefix") String datePrefix);

    List<TrendPoint> selectTranAmountByMonth();

    /**
     * AI 工具：查询指定负责人名下的最近订单（普通用户仅能查询本人数据，范围在 Service 层强制限定）
     */
    List<TTran> selectRecentByCreateBy(@Param("createBy") Integer createBy,
                                       @Param("stage") Integer stage,
                                       @Param("limit") int limit);

    /**
     * AI 工具：查询指定负责人在截止时间前需要跟进联系的交易提醒
     */
    List<TTran> selectUpcomingFollowUp(@Param("createBy") Integer createBy,
                                       @Param("deadline") java.util.Date deadline,
                                       @Param("limit") int limit);

    /**
     * AI 工具（管理员）：跨用户全局订单条件查询（客户姓名模糊 + 阶段精确 + 条数上限）
     */
    List<TTran> selectGlobalByCondition(@Param("customerName") String customerName,
                                        @Param("stage") Integer stage,
                                        @Param("limit") int limit);

    /**
     * AI 付费能力（用户视角）：按负责人统计本人每月的交易金额走势
     */
    List<TrendPoint> selectTranAmountByMonthByCreateBy(@Param("createBy") Integer createBy);
}