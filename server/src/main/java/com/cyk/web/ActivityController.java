package com.cyk.web;

import com.cyk.mapper.TRoleMapper;
import com.cyk.mapper.TUserRoleMapper;
import com.cyk.model.TActivity;
import com.cyk.model.TActivityRemark;
import com.cyk.model.TUser;
import com.cyk.query.ActivityQuery;
import com.cyk.result.R;
import com.cyk.service.ActivityService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
public class ActivityController {

    @Resource
    private ActivityService activityService;
    @Resource
    private TUserRoleMapper tUserRoleMapper;
    @Resource
    private TRoleMapper tRoleMapper;
    /**
     * 市场活动分页查询
     * @param currentPage
     * @return
     */
    @PreAuthorize("hasAuthority('activity:list')")
    @GetMapping("/api/activities")
    public R activityPage(@RequestParam(value = "current", required = false) Integer currentPage, ActivityQuery activityQuery) {
        //required表示current可传可不传
        if (currentPage == null) {
            currentPage = 1;
        }
        PageInfo<TActivity> pageInfo = activityService.getActivityByPage(currentPage, activityQuery);

        return R.OK(pageInfo);
    }

    @PreAuthorize("hasAuthority('activity:edit')")
    @PutMapping("/api/activities/{id}")
    public R editActivity(@PathVariable Integer id,@RequestBody ActivityQuery activityQuery){
        activityQuery.setId(id);
        int i = activityService.editActivity(activityQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('activity:delete')")
    @DeleteMapping("/api/activities/{id}")
    public R deleteActivity(@PathVariable Integer id) {
        int i = activityService.deleteActivityById(id);
        return i == 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('activity:add')")
    @PostMapping("/api/activities")
    public R addActivity( ActivityQuery activityQuery) {
        int i = activityService.insertActivity(activityQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    /**
     * 查询某个活动的所有备注（未删除）
     */
    @GetMapping("api/activities/{activityId}/remarks")
    public R listRemarks(@PathVariable Integer activityId){
        List<TActivityRemark> tActivityRemarks = activityService.listRemarksById(activityId);
        return R.OK(tActivityRemarks);
    }

    /**
     * 添加活动备注
     * @param activityId
     * @param remark
     * @return
     */
    @PostMapping("api/activities/{activityId}/remarks")
    public R addRemark(@PathVariable Integer activityId, TActivityRemark remark) {
        remark.setActivityId(activityId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TUser) {
            TUser currentUser = (TUser) auth.getPrincipal();
            remark.setCreateBy(currentUser.getId());
        }
        remark.setCreateTime(new Date());
        remark.setDeleted(0);
        int result = activityService.insertRemark(remark);
        return result == 1 ? R.OK() : R.FAIL();
    }

    /**
     * 修改备注
     */
    @PutMapping("/api/activities/remarks/{remarkId}")
    public R updateRemark(@PathVariable Integer remarkId, @RequestBody TActivityRemark remark) {
        TActivityRemark existing = activityService.getRemarkById(remarkId);
        if (existing == null || existing.getDeleted() == 1) {
            return R.FAIL("备注不存在");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TUser)) {
            return R.FAIL("未登录");
        }
        TUser currentUser = (TUser) auth.getPrincipal();
        boolean isAdmin = isAdmin(currentUser.getId());
        if (isAdmin) {
            return R.FAIL("管理员不能修改备注");
        }
        if (!existing.getCreateBy().equals(currentUser.getId())) {
            return R.FAIL("只能修改自己创建的备注");
        }
        TActivityRemark updateObj = new TActivityRemark();
        updateObj.setId(remarkId);
        updateObj.setNoteContent(remark.getNoteContent());
        updateObj.setEditBy(currentUser.getId());
        updateObj.setEditTime(new Date());
        int result = activityService.updateRemark(updateObj);
        return result == 1 ? R.OK() : R.FAIL();
    }

    /**
     * 删除备注（逻辑删除）
     */
    @DeleteMapping("/api/activities/remarks/{remarkId}")
    public R deleteRemark(@PathVariable Integer remarkId) {
        TActivityRemark existing = activityService.getRemarkById(remarkId);
        if (existing == null || existing.getDeleted() == 1) {
            return R.FAIL("备注不存在");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TUser)) {
            return R.FAIL("未登录");
        }
        TUser currentUser = (TUser) auth.getPrincipal();
        boolean isAdmin = isAdmin(currentUser.getId());
        if (!isAdmin && !existing.getCreateBy().equals(currentUser.getId())) {
            return R.FAIL("只能删除自己创建的备注");
        }
        int result = activityService.deleteRemarkById(remarkId);
        return result == 1 ? R.OK() : R.FAIL();
    }

    // 辅助方法：判断用户是否为管理员（角色名称为 'admin'）
    private boolean isAdmin(Integer userId) {
        List<Integer> roleIds = tUserRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        List<String> roleNames = tRoleMapper.selectRoleNamesByIds(roleIds);
        return roleNames.contains("admin");
    }
}
