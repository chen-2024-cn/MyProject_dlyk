package com.cyk.mapper;

import com.cyk.model.TUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TUserRoleMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TUserRole record);

    int insertSelective(TUserRole record);

    TUserRole selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TUserRole record);

    int updateByPrimaryKey(TUserRole record);

    List<Integer> selectRoleIdsByUserId(@Param("userId") Integer userId);
}