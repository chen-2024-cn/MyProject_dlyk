package com.cyk.result.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 聊天记录持久化条目（Redis 中按 JSON 数组存储）。
 *
 * 只存「文字气泡」的还原要素：角色 + 内容 + 展示时间。
 * 付费开通卡片 / 文件下载按钮等瞬时交互事件不入库（产品已确认取舍）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatHistoryItem implements Serializable {

    /** 消息角色：user / ai */
    private String role;

    /** 消息正文（纯文本，已剥离事件帧） */
    private String content;

    /** 展示时间（HH:mm:ss） */
    private String time;

    private static final long serialVersionUID = 1L;
}
