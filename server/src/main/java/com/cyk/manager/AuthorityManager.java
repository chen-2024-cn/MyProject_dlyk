package com.cyk.manager;

import com.cyk.constants.Constants;
import com.cyk.mapper.TPermissionMapper;
import com.cyk.mapper.TRoleMapper;
import com.cyk.model.TPermission;
import com.cyk.model.TRole;
import com.cyk.model.TUser;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限装载：角色与按钮权限码永远以服务端（数据库）为准，不再信任令牌里携带的任何权限信息。
 *
 * <p>刻意只依赖 Mapper。它会被 TokenVerifyFilter 注入，而过滤器又被 SecurityConfig 注入；
 * 若这里依赖 UserService（间接依赖 SecurityConfig 里定义的 PasswordEncoder）就会形成循环依赖。</p>
 */
@Component
public class AuthorityManager {

    @Resource
    private TRoleMapper tRoleMapper;

    @Resource
    private TPermissionMapper tPermissionMapper;

    /** 按用户 ID 实时查库，把角色与按钮权限码填进用户对象 */
    public void fill(TUser user) {
        List<String> roleNames = tRoleMapper.selectByUserId(user.getId()).stream()
                .map(TRole::getRole)
                .toList();
        user.setRoleList(roleNames);
        user.setPermissionList(resolveButtonPermissions(user.getId(), roleNames));
    }

    /**
     * 解析生效的按钮权限码：admin 隐式拥有全部，其余严格按绑定表生效。
     * 这条规则原先在 UserServiceImpl 里，迁到这里作为唯一来源，避免两处各写一份后逐渐不一致。
     */
    public List<String> resolveButtonPermissions(Integer userId, List<String> roleList) {
        if (roleList != null && roleList.contains(Constants.ROLE_ADMIN)) {
            return tPermissionMapper.selectAllButtonCodes();
        }
        return tPermissionMapper.selectButtonPermissionByUserId(userId).stream()
                .map(TPermission::getCode)
                .toList();
    }

}