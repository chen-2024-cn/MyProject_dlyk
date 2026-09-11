package com.cyk.web;


import com.cyk.mapper.TClueMapper;
import com.cyk.model.TClue;
import com.cyk.model.TClueRemark;
import com.cyk.model.TDicValue;
import com.cyk.query.ClueQuery;
import com.cyk.query.ClueRemarkQuery;
import com.cyk.result.R;
import com.cyk.service.ClueService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ClueController {

    @Resource
    private ClueService clueService;
    @Autowired
    private TClueMapper tClueMapper;


    /**
     * 线索分页查询（支持多条件筛选）。
     *
     * <p>【功能补强】旧版仅接收页码，前端即使渲染搜索栏也无法生效——线索模块长期只能「翻页浏览」。
     * 现接收 ClueQuery，由 Spring 自动将 URL 查询串绑定到其字段（与 ActivityController 同一约定），
     * 前端可传 fullName / phone / ownerId / source / intentionState / converted 等任意组合，
     * 不传即等价于原全量分页行为，保持向后兼容。</p>
     *
     * <p>安全说明：ClueQuery 继承 BaseQuery 含 filterSQL 字段，但该字段已由
     * {@code SecureBinderAdvice} 全局拒收，外部无法通过 URL 注入 SQL 片段。</p>
     *
     * @param current   页码
     * @param clueQuery 筛选条件（字段全部可选）
     */
    @PreAuthorize("hasAuthority('clue:list')")
    @GetMapping(value = "/api/clues")
    public R cluePage(@RequestParam(value = "current", required = false) Integer current,
                      ClueQuery clueQuery) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TClue> pageInfo = clueService.getClueByPage(current, clueQuery);
        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('clue:import')")
    @PostMapping("/api/importExcel")
    public R importExcel(MultipartFile file, @RequestHeader("Authorization") String token) throws IOException {//接收文件，“file”这个名字必须跟前端formData的名字一致
        clueService.importExcel(file.getInputStream(), token);
        return R.OK();
    }

    /**
     * 检查手机是否重复
     *
     * <p>【安全修复】旧版无任何鉴权，登录态下任意用户（含零角色账号）都可用它
     * 遍历探测系统内是否存在某手机号 —— 线索手机号属客户隐私，构成枚举泄露面。
     * 该接口仅在「新增/编辑线索」表单的手机号 blur 校验时调用，故收敛到
     * clue:add / clue:edit 权限；列表查看者(clue:view)无需触发查重。</p>
     *
     * @param phone
     * @return
     */
    @PreAuthorize("hasAnyAuthority('clue:add','clue:edit')")
    @GetMapping("/api/clue/{value}")
    public R selectPhone(@PathVariable("value") String phone){
        boolean b = clueService.checkPhone(phone);
        return b ? R.OK() : R.FAIL();
    }

    /**
     * 添加
     * @param clueQuery
     * @param token
     * @return
     */
    @PreAuthorize("hasAuthority('clue:add')")
    @PostMapping("/api/clue")
    public R addClue(ClueQuery clueQuery, @RequestHeader("Authorization") String token){
        clueQuery.setToken(token);
        int i = clueService.saveClue(clueQuery);
        return i > 0 ? R.OK() : R.FAIL();
    }

    /**
     * 查询
     * @param id
     * @return
     */
    @PreAuthorize("hasAuthority('clue:view')")
    @GetMapping("/api/clue/detail/{id}")
    public R loadClue(@PathVariable("id") Integer id) {
        TClue clueById = clueService.getClueById(id);
        return R.OK(clueById);
    }

    /**
     * 编辑
     * @param clueQuery
     * @param token
     * @return
     */
    @PreAuthorize("hasAuthority('clue:edit')")
    @PutMapping("/api/clue")
    public R editClue(@RequestBody ClueQuery clueQuery, @RequestHeader("Authorization") String token){
        clueQuery.setToken(token);
        int i = clueService.updateClue(clueQuery);
        return i > 0 ? R.OK() : R.FAIL();
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @PreAuthorize("hasAuthority('clue:delete')")
    @DeleteMapping("/api/clue/{id}")
    public R deleteClue(@PathVariable("id") Integer id) {
        int i = clueService.deleteClue(id);
        return i == 1 ? R.OK() : R.FAIL();
    }

    /**
     * 查询评论
     *
     * <p>【安全修复】跟进记录含客户沟通隐私，读取需与查看线索详情同级权限 clue:view。</p>
     *
     * @param current
     * @param clueId
     * @return
     */
    @PreAuthorize("hasAuthority('clue:view')")
    @GetMapping("/api/clue/remark")
    public R getClueRemark(@RequestParam(value = "current", required = false) Integer current, @RequestParam(value = "clueId") Integer clueId) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TClueRemark> pageInfo = clueService.getClueRemarkByPage(current, clueId);
        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('clue:edit')")
    @PostMapping("/api/clue/remark")
    public R addClueRemark(ClueRemarkQuery clueRemarkQuery, @RequestHeader("Authorization") String token) {
        clueRemarkQuery.setToken(token);
        int i = clueService.addClueRemark(clueRemarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('clue:edit')")
    @PutMapping("/api/clue/remark/update")
    public R editClueRemark(@RequestBody ClueRemarkQuery clueRemarkQuery, @RequestHeader("Authorization") String token) {
        clueRemarkQuery.setToken(token);
        int i = clueService.updateClueRemark(clueRemarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('clue:delete')")
    @DeleteMapping("/api/clue/remark/delete/{id}")
    public R deleteClueRemark(@PathVariable("id") Integer id) {
        int i = clueService.deleteClueRemark(id);
        return i == 1 ? R.OK() : R.FAIL();
    }
    /**
     * 批量删除线索
     */
    @PreAuthorize("hasAuthority('clue:delete')")
    @DeleteMapping("/api/clue/batch")
    public R deleteClueBatch(@RequestParam("ids") String ids) {
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        int i = clueService.deleteClueBatch(idList);
        return i == idList.size() ? R.OK() : R.FAIL();
    }


}

