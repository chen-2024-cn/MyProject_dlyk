package com.cyk.result.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 智能体「身份画像」响应模型：
 * 前端凭此渲染角色专属的工具面板与付费能力商店
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRoleProfile {

    /** 角色标识：ADMIN / USER */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    /** 当前登录人的 AI 角色 */
    private String role;

    /** 当前登录人姓名 */
    private String userName;

    /** 按角色过滤后的能力清单（含开通状态） */
    private List<AiAbilityVO> abilities;
}
