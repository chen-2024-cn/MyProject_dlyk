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


    @PreAuthorize("hasAuthority('clue:list')")
    @GetMapping(value = "/api/clues")
    public R cluePage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TClue> pageInfo = clueService.getClueByPage(current);
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
     * @param phone
     * @return
     */
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
     * @param current
     * @param clueId
     * @return
     */
    @GetMapping("/api/clue/remark")
    public R getClueRemark(@RequestParam(value = "current", required = false) Integer current, @RequestParam(value = "clueId") Integer clueId) {
        if (current == null) {
            current = 1;
        }
        PageInfo<TClueRemark> pageInfo = clueService.getClueRemarkByPage(current, clueId);
        return R.OK(pageInfo);
    }

    @PostMapping("/api/clue/remark")
    public R addClueRemark(ClueRemarkQuery clueRemarkQuery, @RequestHeader("Authorization") String token) {
        clueRemarkQuery.setToken(token);
        int i = clueService.addClueRemark(clueRemarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PutMapping("/api/clue/remark/update")
    public R editClueRemark(@RequestBody ClueRemarkQuery clueRemarkQuery, @RequestHeader("Authorization") String token) {
        clueRemarkQuery.setToken(token);
        int i = clueService.updateClueRemark(clueRemarkQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

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

