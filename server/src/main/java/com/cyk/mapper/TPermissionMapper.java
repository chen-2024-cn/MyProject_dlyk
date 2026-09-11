package com.cyk.mapper;

import com.cyk.model.TPermission;

import java.util.List;

public interface TPermissionMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TPermission record);

    int insertSelective(TPermission record);

    TPermission selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TPermission record);

    int updateByPrimaryKey(TPermission record);

    List<TPermission> selectMenuPermissionByUserId(Integer userId);

    List<TPermission> selectButtonPermissionByUserId(Integer userId);

    /**
     * 查询系统内<b>全部</b>按钮级权限码（type='button'）。
     *
     * <p><b>为何需要</b>：超级管理员（role='admin'）应当隐式拥有所有功能权限，
     * 而不是依赖 {@code t_role_permission} 种子数据是否逐条配全。一旦绑定表缺失某些
     * 权限行（实测环境中 admin 就缺 {@code tran:add/edit/delete}、{@code customer:delete}），
     * 管理员会被自己的权限注解拦在外面，功能直接不可用。</p>
     *
     * <p>本方法为 {@code UserServiceImpl.applyAdminFullPermissions} 提供全量权限码基准，
     * 使权限模型的完整性不再受种子数据遗漏影响。</p>
     */
    List<String> selectAllButtonCodes();
}