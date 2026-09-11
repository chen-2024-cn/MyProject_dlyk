package com.cyk.mapper;

import com.cyk.commons.DataScope;
import com.cyk.model.TActivity;
import com.cyk.query.ActivityQuery;
import com.cyk.query.BaseQuery;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface TActivityMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TActivity record);

    int insertSelective(TActivity record);

    TActivity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TActivity record);

    int updateByPrimaryKey(TActivity record);

    @DataScope(tableAlias = "ta", tableField = "owner_id")
    List<TActivity> selectActivityByPage(ActivityQuery query);

    /**
     * 批量删除活动（前端「批量删除」按钮对应能力）。
     * 旧版该端点在后端完全缺失，前端调用必然落入全局异常兜底。
     */
    int deleteByIds(@Param("list") List<Integer> ids);

    List<TActivity> selectOngoingActivity();

    Collection<Object> selecOngoingActivity();

    Integer selectByCount();
}