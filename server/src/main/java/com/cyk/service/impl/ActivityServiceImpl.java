package com.cyk.service.impl;

import com.cyk.constants.Constants;
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

    @Override
    public int editActivity(ActivityQuery activityQuery) {
        TActivity activity = new TActivity();
        activity.setOwnerId(activityQuery.getOwnerId());
        activity.setName(activityQuery.getName());
        activity.setId(activityQuery.getId());
        activity.setStartTime(activityQuery.getStartTime());
        activity.setEndTime(activityQuery.getEndTime());
        activity.setCost(activityQuery.getBudget());
        activity.setEditTime(new Date());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TUser) {
            TUser currentUser = (TUser) authentication.getPrincipal();
            activity.setEditBy(currentUser.getId());
        }

        return tActivityMapper.updateByPrimaryKeySelective(activity);
    }

    @Override
    public int deleteActivityById(Integer id) {
        return tActivityMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insertActivity(ActivityQuery activityQuery) {
        TActivity activity = new TActivity();
        activity.setOwnerId(activityQuery.getOwnerId());
        activity.setName(activityQuery.getName());
        activity.setStartTime(activityQuery.getStartTime());
        activity.setEndTime(activityQuery.getEndTime());
        activity.setCost(activityQuery.getBudget());
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
