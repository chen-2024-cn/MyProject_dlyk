package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.exception.BusinessException;
import com.cyk.mapper.TProductMapper;
import com.cyk.model.TProduct;
import com.cyk.model.TUser;
import com.cyk.query.ProductQuery;
import com.cyk.service.ProductService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private TProductMapper tProductMapper;

    /** 删除产品被拦截时的友好提示（t_customer.product 外键 RESTRICT） */
    private static final String PRODUCT_REFERENCED_MSG =
            "该产品已被客户的意向产品引用，无法删除；如需下架请将其状态改为「售罄」";

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

    /**
     * 价格与名称合法性校验。
     *
     * <p>【修复的既有缺陷】探针实测：起始价 999 > 最高价 1 的非法区间、-100 的负价
     * 都能直接入库（code=200），后续报价对比、看板金额统计全部被污染。</p>
     *
     * <p><b>刻意不做的校验</b>：①「经销商报价必须落在指导价区间内」——库内 10 个产品
     * 报价全部低于指导价下限，终端优惠是该业务常态，加此校验会把合法数据全部拦死；
     * ②产品名查重——t_product.name 无唯一约束且现存数据无重名，应用层不应臆造库层没有的约束。</p>
     */
    private void validateProduct(ProductQuery query) {
        if (!StringUtils.hasText(query.getName())) {
            throw new BusinessException("产品名称不能为空");
        }
        BigDecimal s = query.getGuidePriceS();
        BigDecimal e = query.getGuidePriceE();
        BigDecimal q = query.getQuotation();
        if (s == null || e == null || q == null) {
            throw new BusinessException("指导价区间与经销商报价均为必填");
        }
        if (s.signum() < 0 || e.signum() < 0 || q.signum() < 0) {
            throw new BusinessException("价格不能为负数");
        }
        if (s.compareTo(e) > 0) {
            throw new BusinessException("官方指导起始价不能高于最高价");
        }
        if (query.getState() != null && query.getState() != 0 && query.getState() != 1) {
            throw new BusinessException("产品状态只能是「在售(0)」或「售罄(1)」");
        }
    }

    @Override
    public int insertProduct(ProductQuery productQuery) {
        validateProduct(productQuery);
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
        validateProduct(productQuery);
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

    /**
     * 删除单个产品。
     *
     * <p>【修复的既有缺陷】t_customer.product 外键为 RESTRICT，删除被客户意向引用的产品
     * （实测产品 id=2 被 4 个客户引用）时直接抛 SQLIntegrityConstraintViolation → 500
     * 「数据库操作失败」。现捕获并转为业务提示，同时给出可执行的替代方案（改售罄下架）——
     * 售罄产品会从 cacheMap 的意向产品下拉中自然消失（DataTask 只缓存 state=0），
     * 既不破坏客户历史数据，又达成下架目的。</p>
     */
    @Override
    public int deleteProductById(Integer id) {
        try {
            return tProductMapper.deleteByPrimaryKey(id);
        } catch (DataAccessException e) {
            log.warn("删除产品失败，疑似被客户意向引用 | productId={}", id, e);
            throw new BusinessException(PRODUCT_REFERENCED_MSG);
        }
    }

    /**
     * 批量删除产品：单事务内逐个删除，任一被引用即整体回滚并提示（不做部分成功，
     * 避免用户难以分辨哪些删了哪些没删）。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchDeleteProducts(List<Integer> ids) {
        if (ObjectUtils.isEmpty(ids)) {
            return 0;
        }
        try {
            return tProductMapper.deleteByIds(ids);
        } catch (DataAccessException e) {
            log.warn("批量删除产品失败，含被客户意向引用的产品 | ids={}", ids, e);
            throw new BusinessException("所选产品中包含被客户意向引用的产品，无法删除；如需下架请改为「售罄」");
        }
    }
}
