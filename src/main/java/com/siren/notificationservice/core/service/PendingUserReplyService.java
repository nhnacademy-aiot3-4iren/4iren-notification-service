package com.siren.notificationservice.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.PendingUserReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 봇이 사용자에게 되물었고 지금 사용자의 대답을 기다리는 지 확인하는 서비스
 * 사용자별 답변 wait 여부를 캐싱합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PendingUserReplyService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String PREFIX = "telegram:pending-reply:user:";

    /**
     * 되묻기 대기 상태 저장
     *
     * @param userId 채팅 유저
     * @param pendingUserReply 봇의 질문과 유저가 구독중인 방 아이디 및 이름 정보
     */
    public boolean save(Long userId, PendingUserReply pendingUserReply) {
        try {
            String json = objectMapper.writeValueAsString(pendingUserReply);
            stringRedisTemplate.opsForValue().set(key(userId), json, TTL);
            return true;
        } catch (Exception e) {
            log.warn("[PendingUserReplyService] 저장 실패 (userId={})", userId, e);
            return false;
        }
    }

    /**
     * 되묻기 대기 상태 조회
     */
    public Optional<PendingUserReply> find(Long userId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(userId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, PendingUserReply.class));
        } catch (Exception e) {
            log.warn("[PendingUserReplyService] 조회 실패, 대기 상태 없는 것으로 취급 (userId={})", userId, e);
            return Optional.empty();
        }
    }

    /**
     * 되묻기 대기 상태를 지운다 (매칭 성공 시).
     */
    public void clear(Long userId) {
        try {
            stringRedisTemplate.delete(key(userId));
        } catch (Exception e) {
            log.warn("[PendingUserReplyService] 삭제 실패, 무시 (userId={})", userId, e);
        }
    }

    /**
     * redis key prefix
     * @param userId 채팅 유저
     * @return redis key
     */
    private String key(Long userId){
        return PREFIX + userId;
    }
}
