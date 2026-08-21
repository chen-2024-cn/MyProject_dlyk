package com.cyk.mapper;

import com.cyk.model.TDicValue;
import com.cyk.query.DicValueQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TDicValueMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TDicValue record);

    int insertSelective(TDicValue record);

    TDicValue selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TDicValue record);

    int updateByPrimaryKey(TDicValue record);

    List<TDicValue> selectDicValueByPage(DicValueQuery query);

    List<TDicValue> selectByTypeCode(@Param("typeCode") String typeCode);
}