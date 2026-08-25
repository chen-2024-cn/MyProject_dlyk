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

    int updateProfile(TUser user);

    int changePassword(Integer userId, String encodedPassword);

    /**
     * 新用户注册
     * @param userQuery 页面表单提交的对应信息
     * @return 1 - 成功， 0 - 失败
     */
    int register(UserQuery userQuery);

    /**
     * 业务层核心逻辑：生成并下发密码重载核验验证码 (内置账户邮箱关联、单账号防刷限流、降级演示打印)
     */
    R generateResetCode(String loginAct, String email);

    /**
     * 业务层核心逻辑：执行密码重设动作 (内置验证码比对、5次错误输入防爆破熔断驱逐保护、DB与Redis清残)
     */
    R executeResetPassword(String loginAct, String code, String newPassword);
}
