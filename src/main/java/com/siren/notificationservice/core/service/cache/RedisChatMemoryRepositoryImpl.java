package com.siren.notificationservice.core.service.cache;

import com.siren.notificationservice.core.dto.StoredMessage;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class RedisChatMemoryRepositoryImpl extends AbstractRedisTtlCache<String,List<StoredMessage>> implements ChatMemoryRepository {
    private static final Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "telegram:chat-memory:intent:";
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
    // KEYS 대신 SCAN을 쓴다 - KEYS는 전체 키 공간을 한 번에 훑어서 그동안 Redis(싱글 스레드)가
    // 다른 요청을 못 받는데, SCAN은 커서로 조금씩 나눠 훑어서 블로킹하지 않는다.
    @Override
    public @NonNull List<String> findConversationIds() {
        List<String> conversationIds = new ArrayList<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(PREFIX + "*").count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            cursor.forEachRemaining(key -> conversationIds.add(key.substring(PREFIX.length())));
        }
        return conversationIds;
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

