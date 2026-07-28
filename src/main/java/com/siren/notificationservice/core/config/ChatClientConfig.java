package com.siren.notificationservice.core.config;

import com.siren.notificationservice.core.service.chat_memory.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
                .model("gemini-flash-latest");

        return ChatClient.builder(geminiChatModel)
                .defaultOptions(liteOptions)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }

    /**
     * 채팅방별 대화 이력 저장소. Redis 기반(RedisChatMemoryRepository)에
     * maxMessages로 최근 윈도우만 유지하는 정책을 얹어 반환한다.
     * ChatClient가 LLM에 요청을 보내기 직전과 직후에 개입하여 메모리를 주입하고 업데이트함
     *
     * @param repo 실제 저장을 담당하는 Redis 구현체
     * @return 최근 6개(약 3턴) 메시지만 유지하는 ChatMemory
     */
    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository repo) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                // 토큰 비용 + Gemini rate limit 고려해 짧게 유지 (전체 이력 아님)
                .maxMessages(6)
                .build();
    }

    
}
