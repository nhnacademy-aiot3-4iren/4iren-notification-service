package com.siren.notificationservice.core.service.alert;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.dto.response.RoomSubscribersResponse;
import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.service.basic_service.AlertHistoryService;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertDispatchServiceTest {

    private final CoreApiClient coreApiClient = mock(CoreApiClient.class);
    private final TelegramSubscriptionService telegramSubscriptionService = mock(TelegramSubscriptionService.class);
    private final AlertMessageFormatter alertMessageFormatter = mock(AlertMessageFormatter.class);
    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final AlertHistoryService alertHistoryService = mock(AlertHistoryService.class);
    private final AlertDigestBufferService alertDigestBufferService = mock(AlertDigestBufferService.class);
    private final AlertDispatchService alertDispatchService = new AlertDispatchService(
            coreApiClient, telegramSubscriptionService, alertMessageFormatter,
            telegramMessageService, alertHistoryService, alertDigestBufferService);

    private AlertEvent event(AlertType type) {
        return new AlertEvent(305L, type, "제목", null, null, null,
                List.of(), Instant.parse("2026-08-06T09:30:00Z"), "evt-1");
    }

    private TelegramSubscription subscription(Long userId, BotType botType) {
        return TelegramSubscription.builder()
                .userId(userId).botType(botType).chatId("chat-" + userId)
                .active(true).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void dispatchUrgentSendsAndRecordsToAdminRecipients() {
        RoomSubscribersResponse response = new RoomSubscribersResponse(305L, "305호",
                List.of(new RoomSubscribersResponse.SubscribersResponse(100L, true)));
        when(coreApiClient.getSubscribers(305L)).thenReturn(response);
        when(telegramSubscriptionService.findActiveAdminSubscriptions(List.of(100L)))
                .thenReturn(List.of(subscription(100L, BotType.ADMIN_BOT)));
        when(alertMessageFormatter.format(any(), eq("305호"))).thenReturn("메시지");
        when(alertHistoryService.findAlreadySentKeys("evt-1")).thenReturn(Set.of());

        alertDispatchService.dispatchUrgent(event(AlertType.SENSOR_ANOMALY));

        verify(telegramMessageService).sendMessage(eq("chat-100"), eq(BotType.ADMIN_BOT), eq("메시지"), any());
        verify(alertHistoryService).saveAll(anyList());
    }

    @Test
    void dispatchUrgentSkipsRecipientsWithNotificationDisabled() {
        RoomSubscribersResponse response = new RoomSubscribersResponse(305L, "305호",
                List.of(new RoomSubscribersResponse.SubscribersResponse(100L, false)));
        when(coreApiClient.getSubscribers(305L)).thenReturn(response);
        when(telegramSubscriptionService.findActiveAdminSubscriptions(List.of())).thenReturn(List.of());

        alertDispatchService.dispatchUrgent(event(AlertType.SENSOR_ANOMALY));

        verify(telegramMessageService, never()).sendMessage(any(), any(), any(), any());
    }

    @Test
    void dispatchDigestBuffersPerUserWithoutSending() {
        RoomSubscribersResponse response = new RoomSubscribersResponse(305L, "305호", List.of(
                new RoomSubscribersResponse.SubscribersResponse(100L, true),
                new RoomSubscribersResponse.SubscribersResponse(101L, true)));
        when(coreApiClient.getSubscribers(305L)).thenReturn(response);

        alertDispatchService.dispatchDigest(event(AlertType.VENTILATION_RECOMMEND));

        verify(alertDigestBufferService, times(2)).buffer(any(), any(AlertDigestBufferEntry.class));
        verify(telegramMessageService, never()).sendMessage(any(), any(), any(), any());
    }
}
