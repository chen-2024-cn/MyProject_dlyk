package com.cyk.service;

import com.cyk.model.TUser;

/**
 * AI 智能体身份解析服务：
 * 将「登录人」翻译为 AI 侧的角色画像（ADMIN / USER），是工具包路由的唯一依据。
 *
 * 安全设计：角色判定完全基于服务端数据（JWT 解析 + 数据库实时查询），
 * 绝不采信前端传入的角色/权限参数，防止越权伪造。
 */
public interface AiRoleResolverService {

    /**
     * 判定当前登录人的 AI 角色。
     * 实时查库获取角色列表（而非信任 JWT 登录时快照），保证角色变更立即生效。
     *
     * @param user JWT 解析出的登录人
     * @return "ADMIN" 或 "USER"
     */
    String resolveRole(TUser user);

    /**
     * 是否为管理员角色（基于实时数据）
     */
    boolean isAdmin(TUser user);
}
