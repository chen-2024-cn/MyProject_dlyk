package com.cyk.service;


import com.cyk.model.TActivity;
import com.cyk.model.TActivityRemark;
import com.cyk.query.ActivityQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ActivityService {
    PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityQuery);

    int editActivity(ActivityQuery activityQuery);

    int deleteActivityById(Integer id);

    int insertActivity(ActivityQuery activityQuery);

    List<TActivityRemark> listRemarksById(Integer activityId);

    int insertRemark(TActivityRemark remark);

    int updateRemark(TActivityRemark remark);

    int deleteRemarkById(Integer remarkId);

    TActivityRemark getRemarkById(Integer id);  // 新增

    List<TActivity> getOngoingActivity();
}
