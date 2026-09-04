package com.cyk.mapper;

import com.cyk.commons.DataScope;
import com.cyk.model.TUser;
import com.cyk.model.TUserRole;
import com.cyk.query.BaseQuery;

import java.util.List;

public interface TUserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TUser record);

    int insertSelective(TUser record);

    TUser selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TUser record);

    int updateByPrimaryKey(TUser record);

    TUser selectByLoginAct(String username);

    /**
     * 仅更新最近登录时间（精准单列更新）。
     * 注意：不要用 updateByPrimaryKeySelective(TUser) 做此操作——
     * 那样会把登录时 JWT 快照里的全量字段（含旧密码、旧姓名）整体回写数据库，
     * 覆盖掉用户在个人中心刚编辑的最新资料。
     */
    int updateLastLoginTimeById(Integer id);

    @DataScope(tableAlias = "tu", tableField = "id")
    List<TUser> selectUserByPage(BaseQuery baseQuery);

    TUser selectDetailById(Integer id);

    int deleteByIds(List list);

    List<TUser> selectByOwner();

    int insertRole(TUserRole userRole);

    /**
     * AI 管理员工具专用：全量用户列表（导出 Excel 用）。
     * 不加 @DataScope：该查询发生在 AI 工具异步线程（无 HTTP 请求上下文），
     * 数据权限切面依赖 RequestContextHolder，工具线程调用会 NPE；
     * 调用入口已通过「管理员角色」硬校验限定只有 admin 能触达。
     */
    List<TUser> selectAllUsers();
}