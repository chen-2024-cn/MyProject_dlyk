package com.cyk.mapper;

import com.cyk.model.TProduct;
import com.cyk.query.ProductQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TProductMapper {
    int deleteByPrimaryKey(Integer id);

    /**
     * 批量删除产品（与其它列表模块能力对齐；全部走 #{} 预编译占位）。
     */
    int deleteByIds(@Param("list") List<Integer> ids);

    int insert(TProduct record);

    int insertSelective(TProduct record);

    TProduct selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TProduct record);

    int updateByPrimaryKey(TProduct record);

    List<TProduct> selectAllOnSaleProduct();

    List<TProduct> selectProductByPage(ProductQuery query);
}