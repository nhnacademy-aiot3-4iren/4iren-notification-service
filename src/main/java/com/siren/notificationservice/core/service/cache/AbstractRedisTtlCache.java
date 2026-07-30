package com.siren.notificationservice.core.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public abstract class AbstractRedisTtlCache<K,T> {
    protected final RedisTemplate<String, T> redisTemplate;

    protected AbstractRedisTtlCache(RedisTemplate<String, T> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    protected abstract String prefix();
    protected abstract Duration ttl();

    public boolean save(K key, T value){
        try{
            redisTemplate.opsForValue().set(buildKey(key), value, ttl());
            return true;
        }catch (Exception e) {
            log.warn("[{}] 저장 실패, 무시 (userId={})", getClass().getSimpleName(), key, e);
            return false;
        }
    }

    public Optional<T> find(K key){
        try{
            return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(key)));
        } catch (Exception e) {
            log.warn("[{}] 조회 실패, 캐시 미스로 취급 (userId={})", getClass().getSimpleName(), key, e);
            return Optional.empty();
        }
    }

    public void clear(K key){
        try{
            redisTemplate.delete(buildKey(key));
        }catch (Exception e) {
            log.warn("[{}] 삭제 실패, 무시 (userId={})", getClass().getSimpleName(), key, e);
        }
    }

    private String buildKey(K key){
        return prefix() + key;
    }

}
