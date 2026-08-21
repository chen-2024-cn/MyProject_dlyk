package com.cyk.mapper;

import com.cyk.model.TDicType;
import com.cyk.query.DicTypeQuery;

import java.util.List;

public interface TDicTypeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TDicType record);

    int insertSelective(TDicType record);

    TDicType selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TDicType record);

    int updateByPrimaryKey(TDicType record);

    List<TDicType> selectByAll();

    List<TDicType> selectDicTypeByPage(DicTypeQuery query);
}