package com.cyk.web;

import com.cyk.model.TProduct;
import com.cyk.query.ProductQuery;
import com.cyk.result.R;
import com.cyk.service.ProductService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
