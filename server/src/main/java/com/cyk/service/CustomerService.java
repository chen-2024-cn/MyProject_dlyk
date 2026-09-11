package com.cyk.service;

import com.cyk.model.TCustomer;
import com.cyk.query.CustomerQuery;
//import com.cyk.result.CustomerExcel;
import com.cyk.result.CustomerExcel;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface CustomerService {

    Boolean convertCustomer(CustomerQuery customerQuery);

    /**
     * 客户分页查询。
     *
     * @param current 页码（从 1 开始）
     * @param query   筛选条件（可为 null）——姓名/手机/负责人/来源/意向状态/选购产品等
     */
    PageInfo<TCustomer> getCustomerByPage(Integer current, CustomerQuery query);

    List<CustomerExcel> getCustomerByExcel(List<String> idList);

    TCustomer getCustomerById(Integer id);

    Boolean deleteCustomer(Integer id);
}
