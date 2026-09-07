package com.cyk.service.impl;

import com.cyk.constants.Constants;
import com.cyk.result.ai.AiChatHistoryItem;
import com.cyk.service.AiChatHistoryService;
import com.cyk.service.RedisService;
import com.cyk.util.JSONUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 聊天记录持久化服务实现。
 *
 * 存储策略：
 * - 单 key 存整段 JSON 数组（dlyk:ai:chat_history:{userId}），读多写少、结构简单；
 * - TTL 与登录态最大值对齐（7 天）：每次追加时刷新，登录态内永不过期丢失；
 * - 容量封顶 100 条：追加时滚动丢弃最早消息，防止单 key 无限膨胀；
 * - 生命周期闭环：退出登录由 MyLogoutSuccessHandler 删除 key；
 *   「清空对话记忆」由 AiAssistantService.resetMemory 调用 clearHistory。
 */
@Slf4j
@Service
public class AiChatHistoryServiceImpl implements AiChatHistoryService {

    @Resource
    private RedisService redisService;

    @Override
    public List<AiChatHistoryItem> listHistory(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        Object raw = redisService.getValue(buildKey(userId));
        if (raw == null) {
            return List.of();
        }
        try {
            List<AiChatHistoryItem> list = JSONUtils.fromJSONArray(raw.toString(), AiChatHistoryItem.class);
            return list != null ? list : List.of();
        } catch (Exception e) {
            // 历史数据损坏不应阻断会话主流程，降级为空历史
            log.warn("AI 聊天记录反序列化失败，已降级为空历史 | userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public void appendHistory(Integer userId, AiChatHistoryItem item) {
        if (userId == null || item == null) {
            return;
        }
        try {
            List<AiChatHistoryItem> list = new ArrayList<>(listHistory(userId));
            list.add(item);
            // 容量封顶：滚动丢弃最早的消息
            while (list.size() > Constants.AI_CHAT_HISTORY_MAX_SIZE) {
                list.remove(0);
            }
            // 原子写入 + 刷新 TTL（与登录态最大有效期对齐）
            redisService.setValue(buildKey(userId), JSONUtils.toJSON(list),
                    Constants.EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 落存失败只影响历史记录功能，绝不影响对话主流程
            log.error("AI 聊天记录落存失败 | userId={}", userId, e);
        }
    }

    @Override
    public void clearHistory(Integer userId) {
        if (userId == null) {
            return;
        }
        redisService.removeValue(buildKey(userId));
        log.info("AI 聊天记录已清空 | userId={}", userId);
    }

    private String buildKey(Integer userId) {
        return Constants.REDIS_AI_CHAT_HISTORY_KEY + userId;
    }
}
