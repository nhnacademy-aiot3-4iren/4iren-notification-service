package com.siren.notificationservice.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.ConversationContext;
import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.dto.RoomWeatherRegion;
import com.siren.notificationservice.core.dto.StoredMessage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
    void roomWeatherRegionRedisTemplateUsesJacksonSerializer() {
        RedisTemplate<String, RoomWeatherRegion> template =
                config.roomWeatherRegionRedisTemplate(connectionFactory, objectMapper);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
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
    void conversationContextRedisTemplateUsesJacksonSerializer() {
        RedisTemplate<String, ConversationContext> template =
                config.conversationContextRedisTemplate(connectionFactory, objectMapper);

        assertThat(template.getConnectionFactory()).isSameAs(connectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
    }
}
