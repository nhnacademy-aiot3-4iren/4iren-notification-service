package com.siren.notificationservice.core.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class AbstractRedisTtlCache<K,T> {
    private static final String KEY_NULL_MESSAGE = "key는 null일 수 없습니다.";

    protected final RedisTemplate<String, T> redisTemplate;

    protected AbstractRedisTtlCache(RedisTemplate<String, T> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    protected abstract String prefix();
    protected abstract Duration ttl();

    public boolean save(K key, T value){
        String redisKey = buildKey(key);
        try{
            redisTemplate.opsForValue().set(redisKey, value, ttl());
            return true;
        }catch (Exception e) {
            log.warn("[{}] 저장 실패, 무시 (userId={})", getClass().getSimpleName(), key, e);
            return false;
        }
    }

    public Optional<T> find(K key){
        String redisKey = buildKey(key);
        try{
            return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey));
        } catch (Exception e) {
            log.warn("[{}] 조회 실패, 캐시 미스로 취급 (userId={})", getClass().getSimpleName(), key, e);
            return Optional.empty();
        }
    }

    public void clear(K key){
        String redisKey = buildKey(key);
        try{
            redisTemplate.delete(redisKey);
        }catch (Exception e) {
            log.warn("[{}] 삭제 실패, 무시 (userId={})", getClass().getSimpleName(), key, e);
        }
    }

    private String buildKey(K key){
        Objects.requireNonNull(key, KEY_NULL_MESSAGE);
        return prefix() + key;
    }

}
