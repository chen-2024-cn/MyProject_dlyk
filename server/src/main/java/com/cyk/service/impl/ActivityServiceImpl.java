package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.exception.BusinessException;
import com.cyk.mapper.TActivityMapper;
import com.cyk.mapper.TActivityRemarkMapper;
import com.cyk.model.TActivity;
import com.cyk.model.TActivityRemark;
import com.cyk.model.TUser;
import com.cyk.query.ActivityQuery;
import com.cyk.query.BaseQuery;
import com.cyk.service.ActivityService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Resource
    private TActivityMapper tActivityMapper;

    @Resource
    private TActivityRemarkMapper tActivityRemarkMapper;
    @Override
    public PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityQuery) {
        //设置pageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        //查询
        List<TActivity> list = tActivityMapper.selectActivityByPage(activityQuery);
        //封装分页数组到PageInfo
        PageInfo<TActivity> info = new PageInfo<>(list);
        return info;
    }

    /**
     * 活动时间区间业务校验：结束时间不得早于开始时间，预算不得为负。
     *
     * <p>原版无任何时间顺序校验，可存入「结束时间早于开始时间」的非法活动；
     * 该类数据会连带污染按时间重叠筛选的结果与看板统计。</p>
     */
    private void validateActivity(ActivityQuery query) {
        if (query.getStartTime() != null && query.getEndTime() != null
                && query.getEndTime().before(query.getStartTime())) {
            throw new BusinessException("活动结束时间不能早于开始时间");
        }
        if (query.getBudget() != null && query.getBudget().signum() < 0) {
            throw new BusinessException("活动预算不能为负数");
        }
        if (!StringUtils.hasText(query.getName())) {
            throw new BusinessException("活动名称不能为空");
        }
    }

    @Override
    public int editActivity(ActivityQuery activityQuery) {
        validateActivity(activityQuery);
        TActivity activity = new TActivity();
        activity.setOwnerId(activityQuery.getOwnerId());
        activity.setName(activityQuery.getName());
        activity.setId(activityQuery.getId());
        activity.setStartTime(activityQuery.getStartTime());
        activity.setEndTime(activityQuery.getEndTime());
        activity.setCost(activityQuery.getBudget());
        // 【修复的既有缺陷】原版未设描述，编辑活动会丢失描述内容
        activity.setDescription(activityQuery.getDescription());
        activity.setEditTime(new Date());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TUser) {
            TUser currentUser = (TUser) authentication.getPrincipal();
            activity.setEditBy(currentUser.getId());
        }

        return tActivityMapper.updateByPrimaryKeySelective(activity);
    }

    /**
     * 删除单个活动。
     *
     * <p>【修复的既有缺陷】原版直接 delete 主记录，但 t_activity_remark.activity_id 外键为
     * RESTRICT，且本系统备注采用逻辑删除（deleted=1 但行仍在），所以凡是有过备注的活动
     * 都删不掉，接口实际返回 code=500（实测已复现）。现先在事务内物理清除备注再删主记录。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteActivityById(Integer id) {
        tActivityRemarkMapper.deleteByActivityId(id);
        return tActivityMapper.deleteByPrimaryKey(id);
    }

    /**
     * 批量删除活动：先一次性级联清理全部备注，再批量删除主记录（单事务保证原子性）。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchDeleteActivities(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        tActivityRemarkMapper.deleteByActivityIds(ids);
        return tActivityMapper.deleteByIds(ids);
    }

    @Override
    public int insertActivity(ActivityQuery activityQuery) {
        validateActivity(activityQuery);
        TActivity activity = new TActivity();
        activity.setOwnerId(activityQuery.getOwnerId());
        activity.setName(activityQuery.getName());
        activity.setStartTime(activityQuery.getStartTime());
        activity.setEndTime(activityQuery.getEndTime());
        activity.setCost(activityQuery.getBudget());
        // 【修复的既有缺陷】活动描述原本从未入库，补齐后新增的活动描述可正常持久化
        activity.setDescription(activityQuery.getDescription());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TUser) {
            TUser currentUser = (TUser) authentication.getPrincipal();
            activity.setCreateBy(currentUser.getId());
        }
        activity.setCreateTime(new Date());

        return tActivityMapper.insertSelective(activity);
    }

    @Override
    public List<TActivityRemark> listRemarksById(Integer activityId) {
        return tActivityRemarkMapper.selectByActivityId(activityId);
    }

    @Override
    public int insertRemark(TActivityRemark remark) {
        int insert = tActivityRemarkMapper.insertSelective(remark);
        return insert;
    }

    @Override
    public int updateRemark(TActivityRemark remark) {
        return tActivityRemarkMapper.updateByPrimaryKeySelective(remark);
    }

    @Override
    public int deleteRemarkById(Integer remarkId) {
        TActivityRemark remark = new TActivityRemark();
        remark.setId(remarkId);
        remark.setDeleted(1);  // 逻辑删除
        return tActivityRemarkMapper.updateByPrimaryKeySelective(remark);
    }

    @Override
    public TActivityRemark getRemarkById(Integer id) {
        return tActivityRemarkMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<TActivity> getOngoingActivity() {
        return tActivityMapper.selectOngoingActivity();
    }
}
