package com.cyk.mapper;

import com.cyk.commons.DataScope;
import com.cyk.model.TActivity;
import com.cyk.query.ActivityQuery;
import com.cyk.query.BaseQuery;

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

    List<TActivity> selectOngoingActivity();

    Collection<Object> selecOngoingActivity();

    Integer selectByCount();
}