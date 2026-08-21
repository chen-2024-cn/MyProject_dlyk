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
}