package com.siren.notificationservice.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.ConversationContext;
import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.dto.RoomWeatherRegion;
import com.siren.notificationservice.core.dto.StoredMessage;
import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisCacheConfigTest {

    private final RedisCacheConfig config = new RedisCacheConfig();
    private final RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void lastMentionedRoomRedisTemplateUsesStringAndLongSerializer() {
        RedisTemplate<String, Long> template = config.lastMentionedRoomRedisTemplate(connectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(GenericToStringSerializer.class);
    }

    @Test
    void feedbackExtractionCacheRedisTemplateUsesJacksonSerializer() {
        RedisTemplate<String, FeedbackExtractionCache> template =
                config.feedbackExtractionCacheRedisTemplate(connectionFactory, objectMapper);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void feedbackExtractionCacheRedisTemplateRoundTripsWithoutTypeMetadata() {
        RedisTemplate<String, FeedbackExtractionCache> template =
                config.feedbackExtractionCacheRedisTemplate(connectionFactory, objectMapper);
        RedisSerializer<FeedbackExtractionCache> valueSerializer =
                (RedisSerializer<FeedbackExtractionCache>) template.getValueSerializer();

        FeedbackExtractionCache original = new FeedbackExtractionCache(
                "301호 너무 더워요",
                List.of(new FeedbackExtractionCache.RoomCandidate(7L, "301호")),
                new FeedbackExtractionResult(
                        List.of(new FeedbackExtractionResult.SensorScore(SensorType.TEMPERATURE, 2)),
                        false, null, null, null, "301호"));
        byte[] serialized = valueSerializer.serialize(original);
        String json = new String(serialized, StandardCharsets.UTF_8);

        assertThat(json).doesNotContain("@class");
        assertThat(valueSerializer.deserialize(serialized)).isEqualTo(original);
    }

    @Test
    void roomWeatherRegionRedisTemplateUsesJacksonSerializer() {
        RedisTemplate<String, RoomWeatherRegion> template =
                config.roomWeatherRegionRedisTemplate(connectionFactory, objectMapper);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void roomWeatherRegionRedisTemplateRoundTripsWithoutTypeMetadata() {
        RedisTemplate<String, RoomWeatherRegion> template =
                config.roomWeatherRegionRedisTemplate(connectionFactory, objectMapper);
        RedisSerializer<RoomWeatherRegion> valueSerializer =
                (RedisSerializer<RoomWeatherRegion>) template.getValueSerializer();

        RoomWeatherRegion original = new RoomWeatherRegion(60, 127);
        byte[] serialized = valueSerializer.serialize(original);
        String json = new String(serialized, StandardCharsets.UTF_8);

        assertThat(json).doesNotContain("@class");
        assertThat(valueSerializer.deserialize(serialized)).isEqualTo(original);
    }

    @Test
    void chatMemoryRedisTemplateUsesJacksonSerializer() {
        RedisTemplate<String, List<StoredMessage>> template =
                config.chatMemoryRedisTemplate(connectionFactory, objectMapper);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatMemoryRedisTemplateRoundTripsWithoutTypeMetadata() {
        RedisTemplate<String, List<StoredMessage>> template =
                config.chatMemoryRedisTemplate(connectionFactory, objectMapper);
        RedisSerializer<List<StoredMessage>> valueSerializer =
                (RedisSerializer<List<StoredMessage>>) template.getValueSerializer();

        List<StoredMessage> original = List.of(
                new StoredMessage(MessageType.USER, "몇 도야?"),
                new StoredMessage(MessageType.ASSISTANT, "지금 24도예요"));
        byte[] serialized = valueSerializer.serialize(original);
        String json = new String(serialized, StandardCharsets.UTF_8);

        assertThat(json).doesNotContain("@class");
        assertThat(valueSerializer.deserialize(serialized)).isEqualTo(original);
    }

    @Test
    void conversationContextRedisTemplateUsesJacksonSerializer() {
        RedisTemplate<String, ConversationContext> template =
                config.conversationContextRedisTemplate(connectionFactory, objectMapper);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
    }

    /**
     * recommendation과 이 값을 Redis로 공유하므로, 직렬화 결과에 @class 같은 타입 메타데이터가
     * 섞이지 않는지(clean JSON)와 원본 record로 정확히 역직렬화되는지를 직접 검증한다.
     */
    @Test
    @SuppressWarnings("unchecked")
    void conversationContextRedisTemplateRoundTripsWithoutTypeMetadata() {
        RedisTemplate<String, ConversationContext> template =
                config.conversationContextRedisTemplate(connectionFactory, objectMapper);
        RedisSerializer<ConversationContext> valueSerializer =
                (RedisSerializer<ConversationContext>) template.getValueSerializer();

        ConversationContext original = new ConversationContext("QUESTION", "몇 도야?", "지금 24도예요");
        byte[] serialized = valueSerializer.serialize(original);
        String json = new String(serialized, StandardCharsets.UTF_8);

        assertThat(json).doesNotContain("@class");
        assertThat(valueSerializer.deserialize(serialized)).isEqualTo(original);
    }
}
