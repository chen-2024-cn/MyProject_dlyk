package com.cyk.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiLangChainConfig {

    @Value("${ai.chat.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${ai.chat.api-key:${AI_API_KEY:}}")
    private String apiKey;

    @Value("${ai.chat.model-name:deepseek-chat}")
    private String modelName;

    @Bean
    public OpenAiChatModel l4jChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                // 安全：关闭请求/响应日志。开启会把含 API-Key 请求头与大模型上下文写入日志，
                // 既泄露密钥又可能落客户隐私数据；排障时可临时打开，生产必须为 false。
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel l4jStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
