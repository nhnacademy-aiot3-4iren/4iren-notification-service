package com.siren.notificationservice.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

import java.time.Duration;


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

    /**
     * 채팅방별 대화 이력을 저장하는 Redis 구현체(공식 spring-ai-model-chat-memory-repository-redis).
     * Redis Stack(RedisJSON+RediSearch)이 필요
     * 커넥션 정보는 이미 있는 spring.data.redis.*(host/port/password/database)를 그대로 재사용해서,
     * 환경별로(로컬: 비밀번호 없음, 배정된 원격 서버: 비밀번호+DB 39) 새 프로퍼티 없이 자동으로 맞춰진다.
     *
     * @param redisProperties 기존 spring.data.redis.* 바인딩(다른 Redis 캐시들과 커넥션 정보 공유)
     * @return 채팅방(conversationId)별 이력을 읽고/쓰는 저장소
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository(DataRedisProperties redisProperties) {
        DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder()
                .database(redisProperties.getDatabase());
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            clientConfig.password(redisProperties.getPassword());
        }

        RedisClient jedisClient = RedisClient.builder()
                .hostAndPort(redisProperties.getHost(), redisProperties.getPort())
                .clientConfig(clientConfig.build())
                .build();

        return RedisChatMemoryRepository.builder()
                .jedisClient(jedisClient)
                .indexName("notification-chat-memory-idx")
                .keyPrefix("telegram:chat-memory:")
                .timeToLive(Duration.ofMinutes(20)) // 기존 커스텀 구현과 동일한 TTL 정책 유지
                .build();
    }

    /**
     * 채팅방별 대화 이력 저장소에 maxMessages로 최근 윈도우만 유지하는 정책을 얹어 반환한다.
     * ChatClient가 LLM에 요청을 보내기 직전과 직후에 개입하여 메모리를 주입하고 업데이트함
     *
     * @param repo 실제 저장을 담당하는 Redis 구현체
     * @return 최근 6개(약 3턴) 메시지만 유지하는 ChatMemory
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repo) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                // 토큰 비용 + Gemini rate limit 고려해 짧게 유지 (전체 이력 아님)
                .maxMessages(6)
                .build();
    }
}
