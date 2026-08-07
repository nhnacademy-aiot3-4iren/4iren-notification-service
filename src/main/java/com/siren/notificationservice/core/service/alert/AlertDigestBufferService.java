package com.siren.notificationservice.core.service.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertDigestFlushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertDigestBufferService {
    private static final String BUFFER_KEY = "notify:user:%d";
    private static final String SCHEDULED_KEY = "notify:user:%d:scheduled";
    private static final String FLUSHING_KEY = "notify:user:%d:flushing";

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.alert-digest-delay}")   private String delayExchange;
    @Value("${rabbitmq.routing-key.alert-digest-delay}") private String delayRoutingKey;
    @Value("${rabbitmq.alert-digest.ttl-ms}")            private long ttlMs;

    /**
     * 버퍼는 redis로 관리할 것
     * userId 별로 buffer를 나누고 RPUSH로 계속 쌓아줌 (메모리기반, 대용량 쓰기 굿)
     */
    public void buffer(Long userId, AlertDigestBufferEntry entry) {
        try{
            String bufferKey = String.format(BUFFER_KEY, userId);
            // 이 유저 버퍼 리스트 끝에 이벤트 추가 (순서 유지를 위해 rpush 씀)
            redisTemplate.opsForList().rightPush(bufferKey, objectMapper.writeValueAsString(entry));

            // 이미 flush 예약이 되어있는 지 확인함
            // true: 없었으니깐 세팅 성공으로 / false: 이미 있음
            Boolean first = redisTemplate.opsForValue()
                    .setIfAbsent(String.format(SCHEDULED_KEY, userId), "1", Duration.ofMillis(ttlMs *2));

            /// 폴링을 없애고 싶어서 컨슈머 없는 큐(대기큐)에 큐 TTL 3분 걸어놓으면 자동으로 dlx 타고 flush 큐로 이동
            /// 여기는 우선 해당 유저 아이디가 redis에 저장되어잇음을 발행(3분)
            if(Boolean.TRUE.equals(first)) {
                // true 면 유저의 첫 발행이므로 delay 큐에 발행
                rabbitTemplate.convertAndSend(delayExchange, delayRoutingKey, new AlertDigestFlushMessage(userId));
            }

        } catch (Exception e) {
            log.warn("[DigestBufferService] 버퍼 적재 실패 (userId={})", userId, e);
        }
    }

    // flush 시점에서 통째로 꺼내기함
    public List<AlertDigestBufferEntry> drain(Long userId) {
        String bufferKey = String.format(BUFFER_KEY, userId);
        String flushingKey = String.format(FLUSHING_KEY, userId);

        try{
            // 버퍼를 원자적으로 통째로 다른 키로 옮김 bufferKey -> flushingKey
            // 처리 중 들어오는 새 이벤트는 새로 생기는 원본 키에 안전하기 쌓이게 돼서 통째로 옮기는게 중요
            redisTemplate.rename(bufferKey, flushingKey); // 버퍼가 비어있으면 예외 -> catch에서 빈리스트
        }catch (Exception e) {
            redisTemplate.delete(String.format(SCHEDULED_KEY, userId));
            return Collections.emptyList();
        }

        List<String> raw = redisTemplate.opsForList().range(flushingKey, 0, -1); // flushing할 걸 List로
        redisTemplate.delete(flushingKey); // flush 지워주고
        redisTemplate.delete(String.format(SCHEDULED_KEY, userId)); // 스케줄 키에도 삭제해줌

        return raw==null ? List.of() : raw.stream()
                                       .map(this::toEntryOrNull)
                                       .filter(Objects::nonNull)
                                       .toList();
    }

    private AlertDigestBufferEntry toEntryOrNull(String json){
        try{
            return objectMapper.readValue(json, AlertDigestBufferEntry.class);
        }catch (Exception e){
            log.warn("[AlertDigestBufferService] 버퍼 항목 역직렬화 실패, 스킵",e);
            return null; // 항목 하나가 깨졌다고 다이제스트 전체를 못 보내면 안돼서 하나만 버리기로..
        }
    }
}
