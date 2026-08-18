package com.siren.notificationservice.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.ConversationContext;
import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.dto.RoomWeatherRegion;
import com.siren.notificationservice.core.dto.StoredMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisCacheConfig {

    /**
     * LastMentionedRoomService 전용 - Long 하나만 문자열로 넣고 빼는 부분
     */
    @Bean
    public RedisTemplate<String, Long> lastMentionedRoomRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Long> mentionTemplate = new RedisTemplate<>();
        mentionTemplate.setConnectionFactory(redisConnectionFactory);
        mentionTemplate.setKeySerializer(new StringRedisSerializer());
        mentionTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class)); //간단하게 Long만 넣고 빼기 때문에
        return mentionTemplate;
    }

    /**
     * FeedbackExtractionCacheService 전용 - 중첩 객체 -> Json으로 직렬화
     */
    @Bean
    public RedisTemplate<String, FeedbackExtractionCache> feedbackExtractionCacheRedisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, FeedbackExtractionCache> feedbackExtractionTemplate = new RedisTemplate<>();
        feedbackExtractionTemplate.setConnectionFactory(redisConnectionFactory);
        feedbackExtractionTemplate.setKeySerializer(new StringRedisSerializer());
        feedbackExtractionTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, FeedbackExtractionCache.class));
        return feedbackExtractionTemplate;
    }

    /**
     * RoomWeatherRegionCacheService 전용 - roomId -> (nx, ny) 캐싱
     */
    @Bean
    public RedisTemplate<String, RoomWeatherRegion> roomWeatherRegionRedisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, RoomWeatherRegion> roomWeatherRegionTemplate = new RedisTemplate<>();
        roomWeatherRegionTemplate.setConnectionFactory(redisConnectionFactory);
        roomWeatherRegionTemplate.setKeySerializer(new StringRedisSerializer());
        roomWeatherRegionTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, RoomWeatherRegion.class));
        return roomWeatherRegionTemplate;
    }

    /**
     * ChatMemoryRepository 직접 커스텀용 redis config
     */
    @Bean
    public RedisTemplate<String, List<StoredMessage>> chatMemoryRedisTemplate (RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, List<StoredMessage>> chatMemoryTemplate = new RedisTemplate<>();
        chatMemoryTemplate.setConnectionFactory(redisConnectionFactory);

        chatMemoryTemplate.setKeySerializer(new StringRedisSerializer());

        chatMemoryTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper,objectMapper.getTypeFactory().constructCollectionType(List.class, StoredMessage.class)));
        return chatMemoryTemplate;
    }

    @Bean
    public RedisTemplate<String, ConversationContext> conversationContextRedisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, ConversationContext> conversationContextTemplate = new RedisTemplate<>();
        conversationContextTemplate.setConnectionFactory(redisConnectionFactory);
        conversationContextTemplate.setKeySerializer(new StringRedisSerializer());
        conversationContextTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, ConversationContext.class));
        return conversationContextTemplate;
    }

}

