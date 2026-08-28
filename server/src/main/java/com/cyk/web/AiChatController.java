package com.cyk.web;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AiChatController {

    @Autowired
    private OpenAiChatModel chatModel;  // 注意：虽然名字带 OpenAi，但实际对接的是 DeepSeek

    @PostMapping("/ai/chat")
    public String chat(@RequestBody ChatRequest request) {
        // 调用模型，AI 答复内容可能较长，日志截断展示前 500 字符，防止日志暴量
        String mes = chatModel.call(request.getMessage());
        log.info("AI 对话响应（原文长度 {}）: {}", mes != null ? mes.length() : 0,
                mes != null && mes.length() > 500 ? mes.substring(0, 500) + "..." : mes);
        return mes;
    }
}

@Data
class ChatRequest {
    private String message;
}
