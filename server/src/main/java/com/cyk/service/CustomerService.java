package com.cyk.service;

import com.cyk.model.TCustomer;
import com.cyk.query.CustomerQuery;
//import com.cyk.result.CustomerExcel;
import com.cyk.result.CustomerExcel;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface CustomerService {

    Boolean convertCustomer(CustomerQuery customerQuery);

    PageInfo<TCustomer> getCustomerByPage(Integer current);

    List<CustomerExcel> getCustomerByExcel(List<String> idList);

    TCustomer getCustomerById(Integer id);

    Boolean deleteCustomer(Integer id);
}
