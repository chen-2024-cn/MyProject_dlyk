package com.cyk.mapper;

import com.cyk.model.TActivityRemark;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TActivityRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TActivityRemark record);

    int insertSelective(TActivityRemark record);

    TActivityRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TActivityRemark record);

    int updateByPrimaryKey(TActivityRemark record);

    List<TActivityRemark> selectByActivityId(@Param("activityId") Integer activityId);
}