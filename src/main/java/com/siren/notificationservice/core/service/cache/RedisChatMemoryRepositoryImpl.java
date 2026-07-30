package com.siren.notificationservice.core.service.cache;

import com.siren.notificationservice.core.dto.StoredMessage;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class RedisChatMemoryRepositoryImpl extends AbstractRedisTtlCache<String,List<StoredMessage>> implements ChatMemoryRepository {
    private static final Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "telegram:chat-memory:";
    private final StringRedisTemplate stringRedisTemplate;

    public RedisChatMemoryRepositoryImpl(@Qualifier("chatMemoryRedisTemplate") RedisTemplate<String, List<StoredMessage>> redisTemplate,
                                         StringRedisTemplate stringRedisTemplate) {
        super(redisTemplate);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected String prefix() {
        return PREFIX;
    }

    @Override
    protected Duration ttl() {
        return TTL;
    }

    // 메서드 단위의 NonNull은 이 메서드는 호출 결과로 절대 null을 반환하지않는 것을 명시적으로 선언하는 마커
    @Override
    public @NonNull List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(PREFIX + "*");
        return keys == null ? List.of() : keys.stream().map(k -> k.substring(PREFIX.length())).toList();
    }

    @Override
    public @NonNull List<Message> findByConversationId(@NonNull String conversationId) {
        return find(conversationId)
                .orElse(List.of()).stream()
                .map(this::toMessage)
                .toList();
    }

    @Override
    public void saveAll(@NonNull String conversationId, List<Message> messages) {
        List<StoredMessage> storedMessages = messages.stream()
                .map(m-> new StoredMessage(m.getMessageType(), m.getText()))
                .toList();
        save(conversationId, storedMessages);
    }

    @Override
    public void deleteByConversationId(@NonNull String conversationId) {
        clear(conversationId);
    }

    private Message toMessage(StoredMessage storedMessage) {
        return switch (storedMessage.type()){
            case USER -> new UserMessage(storedMessage.text());
            case ASSISTANT ->  new AssistantMessage(storedMessage.text());
            case SYSTEM -> new SystemMessage(storedMessage.text());
            case TOOL ->  throw new UnsupportedOperationException("TOOL 메시지는 저장 안 함");
        };
    }
}

