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

    @Value("${ai.chat.api-key:sk-285bfcb58d7d4132ab0639247a233558}")
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
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel l4jStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
