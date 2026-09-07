package com.cyk.service;

import com.cyk.result.ai.AiChatHistoryItem;

import java.util.List;

/**
 * AI 聊天记录持久化服务。
 *
 * 产品语义（已确认）：
 * - 登录态内：记录跟随账号存于 Redis，切换模块/刷新/关浏览器再打开均完整保留；
 * - 退出登录：登出钩子删除 Redis key，记录永久清除；
 * - 被顶下线：记录跟账号不跟设备，新设备可见；
 * - 容量上限：最近 100 条封顶，更早的消息滚动丢弃。
 */
public interface AiChatHistoryService {

    /** 读取指定用户的聊天记录（按时间正序；无记录返回空列表） */
    List<AiChatHistoryItem> listHistory(Integer userId);

    /** 追加一条消息（内部自动裁剪到上限并刷新 TTL 与登录态对齐） */
    void appendHistory(Integer userId, AiChatHistoryItem item);

    /** 清空指定用户的聊天记录（"清空对话记忆"按钮触发） */
    void clearHistory(Integer userId);
}
