package com.cyk.service;

import com.cyk.model.TDicType;
import com.cyk.model.TDicValue;
import com.cyk.query.DicTypeQuery;
import com.cyk.query.DicValueQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface DicTypeService {

    List<TDicType> loadAllDicData();

    PageInfo<TDicType> getDicTypeByPage(Integer current, DicTypeQuery query);

    int insertDicType(DicTypeQuery query);

    int editDicType(DicTypeQuery query);

    int deleteDicTypeById(Integer id);

    PageInfo<TDicValue> getDicValueByPage(Integer current, DicValueQuery query);

    int insertDicValue(DicValueQuery query);

    int editDicValue(DicValueQuery query);

    int deleteDicValueById(Integer id);
}
