package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.mapper.TDicTypeMapper;
import com.cyk.mapper.TDicValueMapper;
import com.cyk.model.TDicType;
import com.cyk.model.TDicValue;
import com.cyk.query.DicTypeQuery;
import com.cyk.query.DicValueQuery;
import com.cyk.service.DicTypeService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DicTypeServiceImpl implements DicTypeService {

    @Resource
    private TDicTypeMapper tDicTypeMapper;

    @Resource
    private TDicValueMapper tDicValueMapper;

    @Override
    public List<TDicType> loadAllDicData() {
        return tDicTypeMapper.selectByAll();
    }

    @Override
    public PageInfo<TDicType> getDicTypeByPage(Integer current, DicTypeQuery query) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TDicType> list = tDicTypeMapper.selectDicTypeByPage(query);
        return new PageInfo<>(list);
    }

    @Override
    public int insertDicType(DicTypeQuery query) {
        TDicType dicType = new TDicType();
        dicType.setTypeCode(query.getTypeCode());
        dicType.setTypeName(query.getTypeName());
        dicType.setRemark(query.getRemark());
        return tDicTypeMapper.insertSelective(dicType);
    }

    @Override
    public int editDicType(DicTypeQuery query) {
        TDicType dicType = new TDicType();
        dicType.setId(query.getId());
        dicType.setTypeCode(query.getTypeCode());
        dicType.setTypeName(query.getTypeName());
        dicType.setRemark(query.getRemark());
        return tDicTypeMapper.updateByPrimaryKeySelective(dicType);
    }

    @Override
    public int deleteDicTypeById(Integer id) {
        return tDicTypeMapper.deleteByPrimaryKey(id);
    }

    @Override
    public PageInfo<TDicValue> getDicValueByPage(Integer current, DicValueQuery query) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TDicValue> list = tDicValueMapper.selectDicValueByPage(query);
        return new PageInfo<>(list);
    }

    @Override
    public int insertDicValue(DicValueQuery query) {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode(query.getTypeCode());
        dicValue.setTypeValue(query.getTypeValue());
        dicValue.setOrder(query.getOrder());
        dicValue.setRemark(query.getRemark());
        return tDicValueMapper.insertSelective(dicValue);
    }

    @Override
    public int editDicValue(DicValueQuery query) {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(query.getId());
        dicValue.setTypeCode(query.getTypeCode());
        dicValue.setTypeValue(query.getTypeValue());
        dicValue.setOrder(query.getOrder());
        dicValue.setRemark(query.getRemark());
        return tDicValueMapper.updateByPrimaryKeySelective(dicValue);
    }

    @Override
    public int deleteDicValueById(Integer id) {
        return tDicValueMapper.deleteByPrimaryKey(id);
    }
}
