package com.cyk.mapper;

import com.cyk.model.TCustomer;
import com.cyk.result.NameValue;

import java.util.List;

public interface TCustomerMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TCustomer record);

    int insertSelective(TCustomer record);

    TCustomer selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TCustomer record);

    int updateByPrimaryKey(TCustomer record);

    List<TCustomer> selectCustomerPage();

    int updateClueIdToNullByClueId(Integer clueId);

    List<TCustomer> selectCustomerByExcel(List<String> idList);

    TCustomer selectCustomerById(Integer id);

    int selectByCount();

    List<NameValue> selectCustomerByMonth();
}