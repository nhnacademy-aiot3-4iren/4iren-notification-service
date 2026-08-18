package com.siren.notificationservice.core.service.cache;

import com.siren.notificationservice.core.dto.ConversationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LlmConversationContextService extends AbstractRedisTtlCache<Long, ConversationContext> {
    private static final Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "telegram:chat-memory:context:";


    public LlmConversationContextService(@Qualifier("conversationContextRedisTemplate") RedisTemplate<String, ConversationContext> redisTemplate) {
        super(redisTemplate);
    }

    @Override
    protected String prefix() {
        return PREFIX;
    }

    @Override
    protected Duration ttl() {
        return TTL;
    }

}
