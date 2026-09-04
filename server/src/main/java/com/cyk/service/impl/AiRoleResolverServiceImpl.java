package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.mapper.TRoleMapper;
import com.cyk.model.TRole;
import com.cyk.model.TUser;
import com.cyk.result.ai.AiRoleProfile;
import com.cyk.service.AiRoleResolverService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 智能体身份解析服务实现。
 *
 * 判定依据：实时查库取角色列表（{@link TRoleMapper#selectByUserId}），
 * 角色标识等于 {@link Constants#ROLE_ADMIN} 即判定为管理员。
 * 不信任 JWT 快照中的角色（登录是历史快照，角色变更后不会同步），
 * 更不信任前端传参——角色是工具包路由的根基，必须来自服务端权威数据。
 */
@Slf4j
@Service
public class AiRoleResolverServiceImpl implements AiRoleResolverService {

    @Resource
    private TRoleMapper tRoleMapper;

    @Override
    public String resolveRole(TUser user) {
        return isAdmin(user) ? AiRoleProfile.ROLE_ADMIN : AiRoleProfile.ROLE_USER;
    }

    @Override
    public boolean isAdmin(TUser user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        List<TRole> roles = tRoleMapper.selectByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(r -> Constants.ROLE_ADMIN.equals(r.getRole()));
    }
}
