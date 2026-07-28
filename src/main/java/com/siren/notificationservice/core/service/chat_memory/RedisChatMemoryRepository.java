package com.siren.notificationservice.core.service.chat_memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.ChatMemoryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 채팅방(chatId)별 대화 이력을 Redis에 TTL로 저장하는 ChatMemoryRepository 구현체.
 * Spring AI 기본 제공 InMemoryChatMemoryRepository는 JVM 메모리라 재시작하면 날아가고
 * 인스턴스가 여러 개면 서로 안 보여서, 이 프로젝트의 다른 단기 상태(LastMentionedRoomService 등)와
 * 같은 Redis TTL 캐시 패턴으로 직접 구현한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "telegram:chat-memory:";


    /**
     * Spring AI 내부에서 전체 대화 목록을 조회할 때 쓰는 메서드인데
     * 이 프로젝트는 conversationId(chatId)를 이미 알고 있는 상태로만 조회 -> 안씀
     */
    @Override
    public List<String> findConversationIds() {
        return List.of();
    }

    /**
     * 특정 채팅방의 저장된 대화 이력을 불러와 Message 리스트로 복원한다.
     * Redis 자체가 죽었거나 저장된 JSON이 깨져있어도(둘 다 TTL 캐시 성격이라 치명적이지 않음)
     * 예외를 던지지 않고 빈 이력으로 취급한다 — 이 메서드는 @RabbitListener 호출 체인
     * (IntentClassificationAgent.classify() 안)에서 실행되므로, 여기서 예외가 새면
     * DLQ 없는 구조상 무한 재큐잉으로 이어진다.
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String json = stringRedisTemplate.opsForValue().get(key(conversationId));
        if (json == null) {
            return List.of();
        }
        try {
            List<ChatMemoryEntry> entries = objectMapper.readValue(json, new TypeReference<List<ChatMemoryEntry>>() {});
            return entries.stream().map(this::toMessage).toList();
        } catch (Exception e) {
            log.warn("[RedisChatMemoryRepository] 조회 실패, 빈 이력으로 취급 (conversationId={})", conversationId, e);
            return List.of();
        }
    }

    /**
     * 이번 턴에 새로 오간 메시지들을 이력에 반영해 Redis에 다시 저장한다.
     * (MessageWindowChatMemory가 maxMessages 윈도우를 적용한 최종 리스트를 넘겨주므로,
     * 여기서는 그 리스트를 그대로 덮어쓰기만 하면 된다.)
     * 저장 실패는 로그만 남기고 무시 - 다음 메시지부터 대화 맥락이 조금 얕아질 뿐,
     * 기능 자체가 멈추면 안 되는 보조 기능이라
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            List<ChatMemoryEntry> entries = messages.stream().map(this::toEntry).toList();
            stringRedisTemplate.opsForValue().set(key(conversationId), objectMapper.writeValueAsString(entries), TTL);
        } catch (Exception e) {
            log.warn("[RedisChatMemoryRepository] 저장 실패, 무시 (conversationId={})", conversationId, e);
        }
    }

    /**
     * 대화 이력을 명시적으로 삭제한다(Spring AI가 필요 시 내부적으로 호출 가능).
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        try {
            stringRedisTemplate.delete(key(conversationId));
        } catch (Exception e) {
            log.warn("[RedisChatMemoryRepository] 삭제 실패, 무시 (conversationId={})", conversationId, e);
        }
    }


    /**
     * Message -> ChatMemoryEntry 변환.
     * SYSTEM/TOOL 타입 메시지는 이 용도(자유 텍스트 의도분류 대화)에서 등장하지 않으므로
     * USER/ASSISTANT 두 종류만 구분하면 충분하다.
     */
    private ChatMemoryEntry toEntry(Message message) {
        String role = (message.getMessageType() == MessageType.ASSISTANT) ? "ASSISTANT" : "USER";
        return new ChatMemoryEntry(role, message.getText());
    }

    /** ChatMemoryEntry -> Message 복원. role이 ASSISTANT가 아니면 전부 USER로 취급. */
    private Message toMessage(ChatMemoryEntry entry) {
        return "ASSISTANT".equals(entry.role()) ? new AssistantMessage(entry.content()) : new UserMessage(entry.content());
    }

    /** Redis key 규칙: 프로젝트 컨벤션대로 prefix + 식별자(chatId) */
    private String key(String conversationId) {
        return PREFIX + conversationId;
    }
}
