package com.siren.notificationservice.core.service.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.siren.notificationservice.core.config.properties.RabbitAlertDigestDelayProperties;
import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertDigestFlushMessage;
import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.entity.domain.AlertType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertDigestBufferServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RabbitAlertDigestDelayProperties alertDigestDelay = new RabbitAlertDigestDelayProperties();
    private final AlertDigestBufferService bufferService =
            new AlertDigestBufferService(redisTemplate, rabbitTemplate, objectMapper, alertDigestDelay);

    @SuppressWarnings("unchecked")
    private final ListOperations<String, String> listOps = mock(ListOperations.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        alertDigestDelay.setExchange("alert.delay");
        alertDigestDelay.setRoutingKey("alert.delay");
        alertDigestDelay.setTtlMs(180000L);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private AlertDigestBufferEntry entry() {
        AlertEvent event = new AlertEvent(305L, AlertType.VENTILATION_RECOMMEND, "환기", null, null, null,
                List.of(), Instant.parse("2026-08-06T09:30:00Z"), "evt-1");
        return new AlertDigestBufferEntry(event, "305호");
    }

    @Test
    void bufferSchedulesFlushOnFirstEvent() {
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        bufferService.buffer(100L, entry());

        verify(rabbitTemplate).convertAndSend(eq("alert.delay"), eq("alert.delay"), any(AlertDigestFlushMessage.class));
    }

    @Test
    void bufferDoesNotScheduleWhenAlreadyScheduled() {
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        bufferService.buffer(100L, entry());

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void drainReturnsBufferedEntries() throws Exception {
        String json = objectMapper.writeValueAsString(entry());
        when(listOps.range("notify:user:100:flushing", 0, -1)).thenReturn(List.of(json));

        List<AlertDigestBufferEntry> result = bufferService.drain(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).roomName()).isEqualTo("305호");
    }

    @Test
    void drainReturnsEmptyWhenBufferMissing() {
        doThrow(new RuntimeException("no such key")).when(redisTemplate).rename(anyString(), anyString());

        List<AlertDigestBufferEntry> result = bufferService.drain(100L);

        assertThat(result).isEmpty();
        verify(listOps, never()).range(anyString(), anyLong(), anyLong());
    }
}
