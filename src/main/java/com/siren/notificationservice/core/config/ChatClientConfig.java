package com.siren.notificationservice.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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

        GoogleGenAiChatOptions.Builder liteOptions = GoogleGenAiChatOptions.builder()
                .model("gemini-3.1-flash-lite");

        return ChatClient.builder(geminiChatModel)
                .defaultOptions(liteOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }


    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(6)
                .build();
    }
}
