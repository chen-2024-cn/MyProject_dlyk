package com.cyk.service.ai;

import lombok.Getter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * AI 智能体请求级上下文（不可变值对象）。
 *
 * 为什么用构造器绑定而不是 ThreadLocal：
 * LangChain4j 的工具执行发生在大模型 HTTP 客户端的异步回调线程上，
 * 并非 Tomcat 请求线程，基于 ThreadLocal 的身份传递会跨线程丢失。
 * 因此在构建 Agent 时把登录人身份"快照式"绑定进上下文对象，工具在任意线程执行都能拿到确定的身份。
 */
@Getter
public class AiAgentContext {

    /** 当前登录人 ID */
    private final Integer userId;

    /** 当前登录人姓名（用于 AI 称谓） */
    private final String userName;

    /** 是否管理员角色（服务端实时判定，非前端传参） */
    private final boolean admin;

    /**
     * 流内系统事件发射器：工具在执行过程中可向前端推送结构化业务事件
     * （如"付费能力未开通"提示），由 SSE 流直接透出，前端解析渲染成交互卡片。
     */
    private final Consumer<String> eventEmitter;

    /** 本轮对话携带的附件文件名（可为 null），由前端先上传、服务端按 fileId 载入 */
    private final String attachmentFileName;

    /** 本轮对话携带的附件内容 Base64（可为 null），供导入类工具直接消费 */
    private final String attachmentBase64;

    /**
     * 附件成功消费后的回调（用于清理会话级附件缓存），可为 null。
     * 背景：前端上传的附件在发送第一条消息后即被一次性消费清空；若大模型选择"先确认再导入"，
     * 用户确认发生在后续轮次，附件不能丢，故服务端按会话暂存，导入成功后才真正移除。
     */
    private final Runnable attachmentConsumedCallback;

    public AiAgentContext(Integer userId, String userName, boolean admin,
                          Consumer<String> eventEmitter,
                          String attachmentFileName, String attachmentBase64,
                          Runnable attachmentConsumedCallback) {
        this.userId = userId;
        this.userName = userName;
        this.admin = admin;
        this.eventEmitter = eventEmitter;
        this.attachmentFileName = attachmentFileName;
        this.attachmentBase64 = attachmentBase64;
        this.attachmentConsumedCallback = attachmentConsumedCallback;
    }

    /**
     * 标记附件已被成功消费（如导入成功后），由服务端清理会话级暂存。
     * 工具在确认附件内容已被真正使用后才调用，确保失败可重试、成功不重复导入。
     */
    public void consumeAttachment() {
        if (attachmentConsumedCallback != null) {
            attachmentConsumedCallback.run();
        }
    }
}
