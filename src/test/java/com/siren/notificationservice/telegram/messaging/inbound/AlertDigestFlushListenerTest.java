package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertDigestFlushMessage;
import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.service.alert.AlertDigestBufferService;
import com.siren.notificationservice.core.service.alert.AlertMessageFormatter;
import com.siren.notificationservice.core.service.basic_service.AlertHistoryService;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertDigestFlushListenerTest {

    private final AlertDigestBufferService alertDigestBufferService = mock(AlertDigestBufferService.class);
    private final TelegramSubscriptionService telegramSubscriptionService = mock(TelegramSubscriptionService.class);
    private final AlertMessageFormatter alertMessageFormatter = mock(AlertMessageFormatter.class);
    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final AlertHistoryService alertHistoryService = mock(AlertHistoryService.class);
    private final AlertDigestFlushListener listener = new AlertDigestFlushListener(
            alertDigestBufferService, telegramSubscriptionService, alertMessageFormatter,
            telegramMessageService, alertHistoryService);

    private AlertDigestBufferEntry entry(String eventId) {
        AlertEvent event = new AlertEvent(305L, AlertType.VENTILATION_RECOMMEND, "환기", null, null, null,
                List.of(), Instant.parse("2026-08-06T09:30:00Z"), eventId);
        return new AlertDigestBufferEntry(event, "305호");
    }

    private TelegramSubscription subscription() {
        return TelegramSubscription.builder()
                .userId(100L).botType(BotType.ADMIN_BOT).chatId("chat-100")
                .active(true).createdAt(ZonedDateTime.now()).build();
    }

    @Test
    void flushSendsDigestAndRecordsHistory() {
        when(alertDigestBufferService.drain(100L)).thenReturn(List.of(entry("evt-1")));
        when(telegramSubscriptionService.findActiveSubscriptions(List.of(100L)))
                .thenReturn(List.of(subscription()));
        when(alertMessageFormatter.formatDigest(anyList())).thenReturn("다이제스트");
        when(alertMessageFormatter.format(any(), any())).thenReturn("요약");
        when(alertHistoryService.findAlreadySentKeys(any())).thenReturn(Set.of());

        listener.flush(new AlertDigestFlushMessage(100L));

        verify(telegramMessageService).sendMessage(any(), any(), any(), any());
        verify(alertHistoryService).saveAll(anyList());
    }

    @Test
    void flushDoesNothingWhenBufferEmpty() {
        when(alertDigestBufferService.drain(100L)).thenReturn(List.of());

        listener.flush(new AlertDigestFlushMessage(100L));

        verify(telegramSubscriptionService, never()).findActiveSubscriptions(anyList());
        verify(telegramMessageService, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    void flushDoesNothingWhenNoSubscriptions() {
        when(alertDigestBufferService.drain(100L)).thenReturn(List.of(entry("evt-1")));
        when(telegramSubscriptionService.findActiveSubscriptions(List.of(100L))).thenReturn(List.of());

        listener.flush(new AlertDigestFlushMessage(100L));

        verify(telegramMessageService, never()).sendMessage(any(), any(), any(), any());
    }
}
