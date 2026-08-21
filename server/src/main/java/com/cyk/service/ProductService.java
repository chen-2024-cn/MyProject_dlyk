package com.cyk.service;

import com.cyk.model.TProduct;
import com.cyk.query.ProductQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ProductService {

    List<TProduct> getAllOnSaleProduct();

    PageInfo<TProduct> getProductByPage(Integer current, ProductQuery productQuery);

    int insertProduct(ProductQuery productQuery);

    int editProduct(ProductQuery productQuery);

    int deleteProductById(Integer id);
}
