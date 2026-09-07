package com.cyk.service;

import com.cyk.model.TUser;
import com.cyk.result.ai.AiChatHistoryItem;
import com.cyk.result.ai.AiRoleProfile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 业务智能体服务。
 *
 * 角色感知设计：身份与权限由服务端从登录人实时判定，
 * 按角色动态挂载不同工具包（普通用户工具包 / 管理员工具包）。
 */
public interface AiAssistantService {

    /**
     * 角色感知的流式对话。
     *
     * @param user             JWT 登录人（服务端权威身份）
     * @param memoryId         前端会话ID（服务端会与用户ID复合，防跨用户读写记忆）
     * @param message          用户消息
     * @param attachmentFileId 本轮对话附件文件ID（先经 /api/ai/file/upload 上传，可为空）
     * @return token 流（含服务端注入的结构化业务事件帧）
     */
    Flux<String> chatStream(TUser user, String memoryId, String message, String attachmentFileId);

    /**
     * 重置指定会话的上下文记忆
     *
     * @param userId   当前登录人（记忆归属校验）
     * @param memoryId 前端会话ID
     */
    void resetMemory(Integer userId, String memoryId);

    /**
     * 构建当前登录人的 AI 角色画像（角色 + 能力清单 + 开通状态），
     * 供前端渲染差异化工具面板与付费能力商店。
     */
    AiRoleProfile buildProfile(TUser user);

    /**
     * 读取指定用户的 AI 聊天记录（登录态内持久化，切换模块/刷新后可恢复）。
     * 按时间正序返回；无记录返回空列表。
     */
    List<AiChatHistoryItem> history(Integer userId);
}
