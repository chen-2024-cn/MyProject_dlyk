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

    /**
     * 统计指定 typeCode 下的字典值数量。
     * 用于删除字典类型前的级联校验：该类型仍有字典值时拒绝删除，
     * 避免直接抛出外键 RESTRICT 异常而变成不友好的 500。
     */
    int countByTypeCode(@Param("typeCode") String typeCode);
}