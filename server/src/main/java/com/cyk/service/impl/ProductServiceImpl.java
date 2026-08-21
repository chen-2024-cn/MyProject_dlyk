package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.mapper.TProductMapper;
import com.cyk.model.TProduct;
import com.cyk.model.TUser;
import com.cyk.query.ProductQuery;
import com.cyk.service.ProductService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private TProductMapper tProductMapper;

    @Override
    public List<TProduct> getAllOnSaleProduct() {
        return tProductMapper.selectAllOnSaleProduct();
    }

    @Override
    public PageInfo<TProduct> getProductByPage(Integer current, ProductQuery productQuery) {
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        List<TProduct> list = tProductMapper.selectProductByPage(productQuery);
        return new PageInfo<>(list);
    }

    @Override
    public int insertProduct(ProductQuery productQuery) {
        TProduct product = new TProduct();
        product.setName(productQuery.getName());
        product.setGuidePriceS(productQuery.getGuidePriceS());
        product.setGuidePriceE(productQuery.getGuidePriceE());
        product.setQuotation(productQuery.getQuotation());
        product.setState(productQuery.getState());
        product.setCreateTime(new Date());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TUser) {
            TUser currentUser = (TUser) authentication.getPrincipal();
            product.setCreateBy(currentUser.getId());
        }
        return tProductMapper.insertSelective(product);
    }

    @Override
    public int editProduct(ProductQuery productQuery) {
        TProduct product = new TProduct();
        product.setId(productQuery.getId());
        product.setName(productQuery.getName());
        product.setGuidePriceS(productQuery.getGuidePriceS());
        product.setGuidePriceE(productQuery.getGuidePriceE());
        product.setQuotation(productQuery.getQuotation());
        product.setState(productQuery.getState());
        product.setEditTime(new Date());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TUser) {
            TUser currentUser = (TUser) authentication.getPrincipal();
            product.setEditBy(currentUser.getId());
        }
        return tProductMapper.updateByPrimaryKeySelective(product);
    }

    @Override
    public int deleteProductById(Integer id) {
        return tProductMapper.deleteByPrimaryKey(id);
    }
}
