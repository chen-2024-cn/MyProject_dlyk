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

    /**
     * 删除某个用户的全部角色绑定（用于删除用户前清理外键关联）
     */
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 批量删除多个用户的全部角色绑定
     */
    int deleteByUserIds(@Param("list") List list);

    /**
     * 批量插入用户角色绑定（一个用户一次分配多个角色）
     */
    int insertBatch(@Param("list") List<TUserRole> list);
}