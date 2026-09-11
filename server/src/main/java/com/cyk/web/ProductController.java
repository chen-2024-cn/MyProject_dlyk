package com.cyk.web;

import com.cyk.model.TProduct;
import com.cyk.query.ProductQuery;
import com.cyk.result.R;
import com.cyk.service.ProductService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController {

    @Resource
    private ProductService productService;

    @PreAuthorize("hasAuthority('product:list')")
    @GetMapping("/api/products")
    public R productPage(@RequestParam(value = "current", required = false) Integer currentPage, ProductQuery productQuery) {
        if (currentPage == null) {
            currentPage = 1;
        }
        PageInfo<TProduct> pageInfo = productService.getProductByPage(currentPage, productQuery);
        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('product:add')")
    @PostMapping("/api/products")
    public R addProduct(ProductQuery productQuery) {
        int i = productService.insertProduct(productQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('product:edit')")
    @PutMapping("/api/products/{id}")
    public R editProduct(@PathVariable Integer id, @RequestBody ProductQuery productQuery) {
        productQuery.setId(id);
        int i = productService.editProduct(productQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('product:delete')")
    @DeleteMapping("/api/products/{id}")
    public R deleteProduct(@PathVariable Integer id) {
        int i = productService.deleteProductById(id);
        return i == 1 ? R.OK() : R.FAIL();
    }

    /**
     * 批量删除产品（与线索/交易/活动/用户列表模块能力对齐）。
     *
     * <p>请求体与市场活动批量删除同款 JSON（{@code {"ids":[1,2,3]}}）。
     * 任一产品被客户意向引用时 Service 抛 BusinessException 整体回滚，
     * 由全局异常处理器透传友好文案。</p>
     */
    @PreAuthorize("hasAuthority('product:delete')")
    @PostMapping("/api/products/batch-delete")
    public R batchDeleteProduct(@RequestBody BatchIdsRequest request) {
        List<Integer> ids = (request == null) ? null : request.getIds();
        if (ObjectUtils.isEmpty(ids)) {
            return R.FAIL("请选择要删除的产品");
        }
        int deleted = productService.batchDeleteProducts(ids);
        return R.OK("已删除 " + deleted + " 个产品");
    }

    /** 批量删除请求体：仅接收 id 列表（与市场活动端点同构，保持前端调用心智一致） */
    static class BatchIdsRequest {
        private List<Integer> ids;
        public List<Integer> getIds() { return ids; }
        public void setIds(List<Integer> ids) { this.ids = ids; }
    }
}
