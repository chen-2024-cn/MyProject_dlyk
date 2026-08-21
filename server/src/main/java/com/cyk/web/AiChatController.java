package com.cyk.web;

import lombok.Data;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiChatController {

    @Autowired
    private OpenAiChatModel chatModel;  // 注意：虽然名字带 OpenAi，但实际对接的是 DeepSeek

    @PostMapping("/ai/chat")
    public String chat(@RequestBody ChatRequest request) {
        // 调用模型
        String mes = chatModel.call(request.getMessage());
        System.out.println(mes);
        return mes;
    }
}

@Data
class ChatRequest {
    private String message;
}
