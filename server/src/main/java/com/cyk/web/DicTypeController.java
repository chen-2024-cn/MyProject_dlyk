package com.cyk.web;

import com.cyk.model.TDicType;
import com.cyk.model.TDicValue;
import com.cyk.query.DicTypeQuery;
import com.cyk.query.DicValueQuery;
import com.cyk.result.R;
import com.cyk.service.DicTypeService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class DicTypeController {

    @Resource
    private DicTypeService dicTypeService;

    @PreAuthorize("hasAuthority('dictype:list')")
    @GetMapping("/api/dictypes")
    public R dicTypePage(@RequestParam(value = "current", required = false) Integer currentPage, DicTypeQuery query) {
        if (currentPage == null) {
            currentPage = 1;
        }
        PageInfo<TDicType> pageInfo = dicTypeService.getDicTypeByPage(currentPage, query);
        return R.OK(pageInfo);
    }

    /**
     * 全部字典类型（不分页，带每类字典值数量）。
     *
     * <p>供「字典类型」目录总览与「字典数据」维护页的类型选择器共用。
     * 权限放宽为拥有 dictype:list 或 dicvalue:list 任一即可——两个菜单分属不同权限组，
     * 只授予字典数据权限的用户也应能在维护页的类型下拉中看到类型列表。</p>
     */
    @PreAuthorize("hasAnyAuthority('dictype:list','dicvalue:list')")
    @GetMapping("/api/dictypes/all")
    public R allDicTypes() {
        return R.OK(dicTypeService.getAllTypes());
    }

    @PreAuthorize("hasAuthority('dictype:add')")
    @PostMapping("/api/dictypes")
    public R addDicType(DicTypeQuery query) {
        int i = dicTypeService.insertDicType(query);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('dictype:edit')")
    @PutMapping("/api/dictypes/{id}")
    public R editDicType(@PathVariable Integer id, @RequestBody DicTypeQuery query) {
        query.setId(id);
        int i = dicTypeService.editDicType(query);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('dictype:delete')")
    @DeleteMapping("/api/dictypes/{id}")
    public R deleteDicType(@PathVariable Integer id) {
        int i = dicTypeService.deleteDicTypeById(id);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('dicvalue:list')")
    @GetMapping("/api/dicvalues")
    public R dicValuePage(@RequestParam(value = "current", required = false) Integer currentPage, DicValueQuery query) {
        if (currentPage == null) {
            currentPage = 1;
        }
        PageInfo<TDicValue> pageInfo = dicTypeService.getDicValueByPage(currentPage, query);
        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('dicvalue:add')")
    @PostMapping("/api/dicvalues")
    public R addDicValue(DicValueQuery query) {
        int i = dicTypeService.insertDicValue(query);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('dicvalue:edit')")
    @PutMapping("/api/dicvalues/{id}")
    public R editDicValue(@PathVariable Integer id, @RequestBody DicValueQuery query) {
        query.setId(id);
        int i = dicTypeService.editDicValue(query);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('dicvalue:delete')")
    @DeleteMapping("/api/dicvalues/{id}")
    public R deleteDicValue(@PathVariable Integer id) {
        int i = dicTypeService.deleteDicValueById(id);
        return i == 1 ? R.OK() : R.FAIL();
    }
}
