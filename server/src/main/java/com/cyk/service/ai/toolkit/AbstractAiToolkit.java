package com.cyk.service.ai.toolkit;

import com.cyk.result.ai.AiAbility;
import com.cyk.service.AiPremiumAbilityService;
import com.cyk.service.ai.AiAgentContext;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 工具集基类：收口通用能力（身份上下文、付费墙校验、系统事件推送）。
 *
 * 付费墙安全模型：
 * 大模型可能受 Prompt 诱导调用付费工具，因此付费校验必须发生在
 * 工具执行层（服务端），而不是依赖模型自觉或前端开关。
 * 未开通时：向前端推送「快捷开通卡片」事件 + 向模型返回引导文案，双管齐下。
 */
@Slf4j
public abstract class AbstractAiToolkit {

    /** 付费能力未开通时推送给前端的结构化事件前缀（前端解析并渲染开通卡片） */
    public static final String EVENT_PREMIUM_REQUIRED_PREFIX = "[AI-EVENT:PREMIUM_REQUIRED:";

    /** 文件就绪事件前缀（前端解析后渲染下载按钮，事件体为文件名） */
    public static final String EVENT_FILE_READY_PREFIX = "[AI-EVENT:FILE_READY:";

    protected final AiAgentContext context;
    protected final AiPremiumAbilityService premiumAbilityService;

    protected AbstractAiToolkit(AiAgentContext context, AiPremiumAbilityService premiumAbilityService) {
        this.context = context;
        this.premiumAbilityService = premiumAbilityService;
    }

    /**
     * 付费能力闸口：已开通返回 null，未开通返回给模型的引导文案（同时向前端推送开通卡片事件）。
     */
    protected String premiumGate(AiAbility ability) {
        if (premiumAbilityService.isGranted(context.getUserId(), ability)) {
            return null;
        }
        // 向前端推送结构化事件（确定性透出，不依赖大模型转述）
        context.getEventEmitter().accept(EVENT_PREMIUM_REQUIRED_PREFIX + ability.getKey() + "]");
        log.info("AI 付费墙拦截 | userId={}, ability={}", context.getUserId(), ability.getKey());
        return String.format(
                "【付费能力未开通】「%s」为增值付费能力（¥%s）。系统已向用户推送快捷开通卡片，"
                        + "请礼貌告知用户该能力需要开通后才能使用，并引导其点击对话中的开通卡片完成购买，购买成功后重新发送指令即可。",
                ability.getName(), ability.getPrice());
    }
}
