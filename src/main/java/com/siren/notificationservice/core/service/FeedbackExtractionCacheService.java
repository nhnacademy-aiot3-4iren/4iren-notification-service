package com.siren.notificationservice.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * FEEDBACK 강의실이 확정되기 전까지, 미리 추출해둔 축별 점수/원문/체감시각을 유저별로
 * 임시 보관하는 캐시. 라우팅(어디로 보낼지)은 콜백 접두사(CallbackActionType)가 담당하고,
 * 이 서비스는 FeedbackCallbackHandler가 강의실 확정 처리 시 참고할 데이터만 들고 있는다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackExtractionCacheService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String PREFIX = "telegram:feedback-extraction-cache:user:";

    /**
     * 강의실 확정 전까지의 추출 결과 저장
     *
     * @param userId 채팅 유저
     * @param cache 피드백 원문 + 후보 강의실 목록 + 추출된 축별 점수/시각 정보
     */
    public boolean save(Long userId, FeedbackExtractionCache cache) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            stringRedisTemplate.opsForValue().set(key(userId), json, TTL);
            return true;
        } catch (Exception e) {
            log.warn("[FeedbackExtractionCacheService] 저장 실패 (userId={})", userId, e);
            return false;
        }
    }

    /**
     * 강의실 확정 전까지의 추출 결과 조회
     */
    public Optional<FeedbackExtractionCache> find(Long userId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(userId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, FeedbackExtractionCache.class));
        } catch (Exception e) {
            log.warn("[FeedbackExtractionCacheService] 조회 실패, 캐시 없는 것으로 취급 (userId={})", userId, e);
            return Optional.empty();
        }
    }

    /**
     * 강의실이 확정돼 더 이상 필요 없어진 캐시를 지운다.
     */
    public void clear(Long userId) {
        try {
            stringRedisTemplate.delete(key(userId));
        } catch (Exception e) {
            log.warn("[FeedbackExtractionCacheService] 삭제 실패, 무시 (userId={})", userId, e);
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
