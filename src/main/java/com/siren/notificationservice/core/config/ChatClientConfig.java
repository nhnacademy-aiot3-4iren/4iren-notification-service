package com.siren.notificationservice.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class ChatClientConfig {

    @Bean
    @Primary
    public ChatClient.Builder ollamaChatClientBuilder(@Qualifier("ollamaChatModel")ChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }

    @Bean
    public ChatClient.Builder geminiChatClientBuilder(@Qualifier("googleGenAiChatModel") ChatModel geminiChatModel) {
        return ChatClient.builder(geminiChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }
    @Bean
    public ChatClient.Builder geminiJsonChatClientBuilder(
            @Qualifier("googleGenAiChatModel") ChatModel geminiChatModel) {

        GoogleGenAiChatOptions liteOptions = GoogleGenAiChatOptions.builder()
                .model("gemini-2.0-flash")
                .build();

        return ChatClient.builder(geminiChatModel)
                .defaultOptions(liteOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }
}
