package com.siren.notificationservice.core.service.cache;

import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * FEEDBACK 강의실이 확정되기 전까지, 미리 추출해둔 축별 점수/원문/체감시각을 유저별로
 * 임시 보관하는 캐시. 라우팅(어디로 보낼지)은 콜백 접두사(CallbackActionType)가 담당하고,
 * 이 서비스는 FeedbackCallbackHandler가 강의실 확정 처리 시 참고할 데이터만 들고 있는다.
 */
@Service
@Slf4j
public class FeedbackExtractionCacheService extends AbstractRedisTtlCache<Long,FeedbackExtractionCache> {
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String PREFIX = "telegram:feedback-extraction-cache:user:";

    public FeedbackExtractionCacheService(@Qualifier("feedbackExtractionCacheRedisTemplate") RedisTemplate<String, FeedbackExtractionCache> redisTemplate) {
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
