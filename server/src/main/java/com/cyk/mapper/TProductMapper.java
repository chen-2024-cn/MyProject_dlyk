package com.cyk.mapper;

import com.cyk.model.TProduct;
import com.cyk.query.ProductQuery;

import java.util.List;

public interface TProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TProduct record);

    int insertSelective(TProduct record);

    TProduct selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TProduct record);

    int updateByPrimaryKey(TProduct record);

    List<TProduct> selectAllOnSaleProduct();

    List<TProduct> selectProductByPage(ProductQuery query);
}