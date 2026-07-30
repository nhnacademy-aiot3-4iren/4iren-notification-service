package com.siren.notificationservice.core.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 사용자별 마지막으로 언급된 방이름을 캐싱합니다.
 */
@Service
@Slf4j
public class LastMentionedRoomService extends AbstractRedisTtlCache<Long,Long>{

    private static final  Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "telegram:last-mentioned-room:";

    public LastMentionedRoomService(@Qualifier("lastMentionedRoomRedisTemplate") RedisTemplate<String,Long> redisTemplate) {
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
