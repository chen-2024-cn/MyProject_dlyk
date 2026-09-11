package com.cyk.service;

import com.cyk.model.TDicType;
import com.cyk.model.TDicValue;
import com.cyk.query.DicTypeQuery;
import com.cyk.query.DicValueQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface DicTypeService {

    List<TDicType> loadAllDicData();

    /**
     * 全部字典类型（不分页，带每类字典值数量）。
     * 供字典类型目录总览与字典数据维护页的类型选择器共用。
     */
    List<TDicType> getAllTypes();

    PageInfo<TDicType> getDicTypeByPage(Integer current, DicTypeQuery query);

    int insertDicType(DicTypeQuery query);

    int editDicType(DicTypeQuery query);

    int deleteDicTypeById(Integer id);

    PageInfo<TDicValue> getDicValueByPage(Integer current, DicValueQuery query);

    int insertDicValue(DicValueQuery query);

    int editDicValue(DicValueQuery query);

    int deleteDicValueById(Integer id);
}
