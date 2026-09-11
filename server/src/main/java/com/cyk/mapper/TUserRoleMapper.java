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

    /**
     * 按用户 ID 集合批量查询角色绑定。
     * <p>用户列表页需展示每个账号的角色，若逐个用户调 {@code selectRoleIdsByUserId}
     * 会造成「一页 10 行 = 10 次额外查询」的 N+1 问题（同类缺陷曾在权限按钮
     * 校验中引发请求风暴）。改为一次拉取本页全部绑定后内存分组。</p>
     */
    List<TUserRole> selectByUserIds(@Param("list") List<Integer> userIds);
}