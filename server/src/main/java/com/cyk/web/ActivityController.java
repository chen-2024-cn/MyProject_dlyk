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

    /**
     * 批量删除活动。
     *
     * <p>【修复的既有缺陷】前端「批量删除」按钮一直调用 {@code POST /api/activities/batch-delete}，
     * 但后端从未实现该端点，请求落入全局异常处理返回 500（实测已复现）。
     * 此处补齐，Service 层会在单事务内级联清理备注后批量删除。</p>
     */
    @PreAuthorize("hasAuthority('activity:delete')")
    @PostMapping("/api/activities/batch-delete")
    public R batchDeleteActivity(@RequestBody BatchIdsRequest request) {
        List<Integer> ids = request == null ? null : request.getIds();
        if (ids == null || ids.isEmpty()) {
            return R.FAIL("请选择要删除的活动");
        }
        int deleted = activityService.batchDeleteActivities(ids);
        return R.OK("已删除 " + deleted + " 个活动");
    }

    /** 批量删除请求体：仅接收 id 列表 */
    static class BatchIdsRequest {
        private List<Integer> ids;
        public List<Integer> getIds() { return ids; }
        public void setIds(List<Integer> ids) { this.ids = ids; }
    }

    @PreAuthorize("hasAuthority('activity:add')")
    @PostMapping("/api/activities")
    public R addActivity( ActivityQuery activityQuery) {
        int i = activityService.insertActivity(activityQuery);
        return i == 1 ? R.OK() : R.FAIL();
    }

    /**
     * 查询某个活动的所有备注（未删除）
     *
     * <p>【安全修复】原版无任何鉴权，同于线索/交易备注的早期缺陷——登录态下任意
     * 用户都可读取活动跟进记录（含客户/活动隐私）。读取收敛到 activity:view（与查看详情同级）。</p>
     */
    @PreAuthorize("hasAuthority('activity:view')")
    @GetMapping("api/activities/{activityId}/remarks")
    public R listRemarks(@PathVariable Integer activityId){
        List<TActivityRemark> tActivityRemarks = activityService.listRemarksById(activityId);
        return R.OK(tActivityRemarks);
    }

    /**
     * 添加活动备注
     *
     * <p>【安全修复】原版无鉴权，与线索/交易备注加固保持一致，收敛到 activity:edit。</p>
     */
    @PreAuthorize("hasAuthority('activity:edit')")
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
    @PreAuthorize("hasAuthority('activity:edit')")
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
        // 【修复的逻辑倒置】原版对管理员直接返回「管理员不能修改备注」，而删除接口却允许管理员
        // 删任意备注——两者自相矛盾且违反管理职责。现与删除保持一致：管理员可编辑任意备注，
        // 普通用户仅限本人创建的备注。
        if (!isAdmin && !existing.getCreateBy().equals(currentUser.getId())) {
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
    @PreAuthorize("hasAuthority('activity:delete')")
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
