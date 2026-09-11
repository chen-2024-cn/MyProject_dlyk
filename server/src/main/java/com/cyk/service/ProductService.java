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

    /**
     * 批量删除产品。
     *
     * @return 实际删除数；任一产品被客户引用时整体失败（BusinessException，事务回滚）
     */
    int batchDeleteProducts(List<Integer> ids);
}
