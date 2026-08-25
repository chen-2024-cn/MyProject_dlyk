package com.cyk.mapper;

import com.cyk.model.TRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TRoleMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TRole record);

    int insertSelective(TRole record);

    TRole selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TRole record);

    int updateByPrimaryKey(TRole record);

    List<TRole> selectByUserId(Integer userId);

    List<String> selectRoleNamesByIds(@Param("roleIds") List<Integer> roleIds);

    /**
     * 查询全部角色（用于用户管理页面分配角色时的下拉选项）
     */
    List<TRole> selectRoleList();
}