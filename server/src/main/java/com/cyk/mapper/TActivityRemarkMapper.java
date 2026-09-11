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

    /**
     * 按活动 ID 物理删除其全部备注（含逻辑删除 deleted=1 的残留行）。
     * <p>删除活动前必须调用：t_activity_remark.activity_id 外键为 RESTRICT，
     * 而本系统的备注删除是逻辑删除（行仍在），故直接删除带备注的活动会触发外键异常
     * （接口实测 code=500）。先物理清除备注再删活动，保证主记录可被正常移除。</p>
     */
    int deleteByActivityId(@Param("activityId") Integer activityId);

    /**
     * 按活动 ID 列表批量物理删除备注，供活动批量删除时统一级联清理。
     */
    int deleteByActivityIds(@Param("list") List<Integer> activityIds);
}