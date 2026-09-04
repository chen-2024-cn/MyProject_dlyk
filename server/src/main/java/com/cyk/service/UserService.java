package com.cyk.service;

import com.cyk.model.TRole;
import com.cyk.model.TUser;
import com.cyk.query.UserQuery;
import com.cyk.result.R;
import com.github.pagehelper.PageInfo;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    PageInfo<TUser> getUserByPage(Integer current);

    TUser getUserById(Integer id);

    int saveUser(UserQuery userQuery);

    int deleteById(Integer id);

    int updateUser(UserQuery userQuery);

    int batchDelUserId(List ids);

    List<TUser> getOwnerList();

    /**
     * 查询全部角色（用户管理页面分配角色时的下拉选项）
     */
    List<TRole> getRoleList();

    void updateLastLoginTime(TUser tUser);

    /**
     * 获取登录人的最新完整信息（实时查库，而不是用登录时 JWT 快照）。
     * 包含：基础字段（数据库最新值）、角色、菜单权限、按钮权限。
     */
    TUser getUserInfo(Integer userId);

    int updateProfile(TUser user);

    int changePassword(Integer userId, String encodedPassword);

    /**
     * 新用户注册
     * @param userQuery 页面表单提交的对应信息
     * @return 1 - 成功， 0 - 失败
     */
    int register(UserQuery userQuery);

    /**
     * AI 管理员工具 · 批量导入用户（登录账号已存在则跳过）。
     * 调用入口由管理员工具包限定，仅 admin 角色可达。
     *
     * @param rows       解析后的导入行
     * @param operatorId 操作管理员用户ID
     * @return 实际导入成功条数
     */
    int aiImportUsers(java.util.List<com.cyk.result.ai.UserImportRow> rows, Integer operatorId);

    /**
     * AI 管理员工具 · 更新用户启停状态并重新分配角色（事务保障）。
     * 账号被禁用时同步清理其登录 token，强制立即下线。
     *
     * @param targetUserId   被操作的用户ID
     * @param accountEnabled 启用状态（1启用 0禁用），null 表示不修改
     * @param roleIds        新的角色ID列表，null 表示不修改角色
     * @param operatorId     操作管理员用户ID
     */
    R aiUpdateUserRolesAndStatus(Integer targetUserId, Integer accountEnabled,
                                 java.util.List<Integer> roleIds, Integer operatorId);

    /**
     * 业务层核心逻辑：生成并下发密码重载核验验证码 (内置账户邮箱关联、单账号防刷限流、降级演示打印)
     */
    R generateResetCode(String loginAct, String email);

    /**
     * 业务层核心逻辑：执行密码重设动作 (内置验证码比对、5次错误输入防爆破熔断驱逐保护、DB与Redis清残)
     */
    R executeResetPassword(String loginAct, String code, String newPassword);
}
