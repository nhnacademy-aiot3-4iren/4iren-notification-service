package com.siren.notificationservice.core.service.cache;

import com.siren.notificationservice.core.dto.RoomWeatherRegion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * roomId → 기상청 격자 좌표(nx, ny) 캐시. Core 날씨 호출을 건너뛸 수 있는지 판단하는 데 쓴다.
 * TTL 7일: 위치는 거의 안 바뀌지만 완전 영구 고정은 피한다.
 */
@Service
@Slf4j
public class RoomWeatherRegionCacheService extends AbstractRedisTtlCache<Long, RoomWeatherRegion> {

    private static final Duration TTL = Duration.ofDays(7);
    private static final String PREFIX = "weather:room-region:";

    public RoomWeatherRegionCacheService(@Qualifier("roomWeatherRegionRedisTemplate") RedisTemplate<String, RoomWeatherRegion> redisTemplate) {
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
