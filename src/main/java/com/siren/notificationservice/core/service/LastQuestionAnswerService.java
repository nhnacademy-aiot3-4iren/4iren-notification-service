package com.siren.notificationservice.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.LastQuestionAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 유저별 마지막 질문/답변 쌍을 짧게 캐싱한다.
 * "아니 302호"처럼 사용자가 직전 답변을 정정하는 짧은 후속 메시지가 왔을 때,
 * Recommendation API가 그 메시지만으론 무슨 질문인지 알 수 없으므로 이 맥락을 같이 넘겨주기 위한 용도.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LastQuestionAnswerService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 정정은 보통 답변을 본 직후 바로 오므로 LastMentionedRoomService(20분)보다 짧게 잡음
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String PREFIX = "telegram:last-qa:";

    /** QUESTION 응답을 보낸 직후 호출해서 다음 후속 메시지를 위한 맥락을 남긴다. */
    public void save(Long userId, String question, String answer) {
        try {
            String json = objectMapper.writeValueAsString(new LastQuestionAnswer(question, answer));
            stringRedisTemplate.opsForValue().set(key(userId), json, TTL);
        } catch (Exception e) {
            log.warn("[LastQuestionAnswerService] 저장 실패, 무시 (userId={})", userId, e);
        }
    }

    /** Recommendation API 호출 전에 조회해서 요청에 같이 실어 보낸다. 없으면(캐시 미스/최초 질문) empty. */
    public Optional<LastQuestionAnswer> find(Long userId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(userId));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, LastQuestionAnswer.class));
        } catch (Exception e) {
            log.warn("[LastQuestionAnswerService] 조회 실패, 없는 것으로 취급 (userId={})", userId, e);
            return Optional.empty();
        }
    }

    private String key(Long userId) {
        return PREFIX + userId;
    }
}
