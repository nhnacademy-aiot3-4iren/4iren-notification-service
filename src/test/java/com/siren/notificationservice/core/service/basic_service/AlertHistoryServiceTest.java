package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.AlertHistoryKey;
import com.siren.notificationservice.core.dto.request.AlertHistorySearchCondition;
import com.siren.notificationservice.core.dto.response.AlertHistoryFilterOptionsResponse;
import com.siren.notificationservice.core.dto.response.AlertHistoryResponse;
import com.siren.notificationservice.core.dto.response.RoomSubResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import com.siren.notificationservice.core.exception.NotFoundAlertHistoryException;
import com.siren.notificationservice.core.repository.AlertHistoryRepository;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertHistoryServiceTest {

    private final AlertHistoryRepository alertHistoryRepository = mock(AlertHistoryRepository.class);
    private final TelegramSubscriptionRepository telegramSubscriptionRepository = mock(TelegramSubscriptionRepository.class);
    private final CoreApiClient coreApiClient = mock(CoreApiClient.class);
    private final AlertHistoryService alertHistoryService = new AlertHistoryService(alertHistoryRepository, telegramSubscriptionRepository, coreApiClient);

    private AlertHistory history(Long id, Long userId, BotType botType, String eventId) {
        return AlertHistory.builder()
                .alertHistoryId(id)
                .roomId(7L)
                .botType(botType)
                .alertType(AlertType.SENSOR_ANOMALY)
                .eventId(eventId)
                .message("센서 이상")
                .sendAt(LocalDateTime.now())
                .userId(userId)
                .build();
    }

    @Test
    void getAlertHistoryByUserIdMapsToResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        AlertHistorySearchCondition condition = new AlertHistorySearchCondition(null, null, null, null, null);
        Page<AlertHistory> page = new PageImpl<>(List.of(history(1L, 100L, BotType.ADMIN_BOT, "evt-1")), pageable, 1);

        when(alertHistoryRepository.search(100L, condition, pageable)).thenReturn(page);

        Page<AlertHistoryResponse> result = alertHistoryService.getAlertHistoryByUserId(100L, condition, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).message()).isEqualTo("센서 이상");
        assertThat(result.getContent().get(0).botType()).isEqualTo("ADMIN_BOT");
    }

    @Test
    void getAlertHistoryByIdReturnsOwnedHistory() {
        when(alertHistoryRepository.findByAlertHistoryIdAndUserId(1L, 100L))
                .thenReturn(Optional.of(history(1L, 100L, BotType.ADMIN_BOT, "evt-1")));

        AlertHistoryResponse response = alertHistoryService.getAlertHistoryById(1L, 100L);

        assertThat(response.alertHistoryId()).isEqualTo(1L);
    }

    @Test
    void getAlertHistoryByIdThrowsWhenNotOwnedOrMissing() {
        when(alertHistoryRepository.findByAlertHistoryIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertHistoryService.getAlertHistoryById(1L, 999L))
                .isInstanceOf(NotFoundAlertHistoryException.class);
    }

    @Test
    void findAlreadySentKeysMapsToUserAndBotType() {
        when(alertHistoryRepository.findByEventId("evt-1")).thenReturn(List.of(
                history(1L, 100L, BotType.ADMIN_BOT, "evt-1"),
                history(2L, 100L, BotType.USER_BOT, "evt-1")
        ));

        Set<AlertHistoryKey> keys = alertHistoryService.findAlreadySentKeys("evt-1");

        assertThat(keys).containsExactlyInAnyOrder(
                new AlertHistoryKey(100L, BotType.ADMIN_BOT),
                new AlertHistoryKey(100L, BotType.USER_BOT));
    }

    @Test
    void saveAllDelegatesToRepository() {
        List<AlertHistory> histories = List.of(history(1L, 100L, BotType.ADMIN_BOT, "evt-1"));

        alertHistoryService.saveAll(histories);

        verify(alertHistoryRepository).saveAll(histories);
    }

    @Test
    void getFilterOptionsReturnsConnectedBotsAndReceivedAlertTypes() {
        when(telegramSubscriptionRepository.findActiveBotTypesByUserId(100L))
                .thenReturn(List.of(BotType.ADMIN_BOT, BotType.USER_BOT));
        when(alertHistoryRepository.findAlertTypesByUserId(100L))
                .thenReturn(List.of(AlertType.SENSOR_ANOMALY, AlertType.VENTILATION_RECOMMEND));
        when(coreApiClient.getRoomSubscriptions(100L)).thenReturn(new UserRoomSubResponse(100L, List.of(
                new RoomSubResponse(7L, "302호", true),
                new RoomSubResponse(9L, "강의실 A", false)
        )));

        AlertHistoryFilterOptionsResponse options = alertHistoryService.getFilterOptions(100L);

        assertThat(options.botTypeList()).containsExactly("ADMIN_BOT", "USER_BOT");
        assertThat(options.alertTypeList()).containsExactly("SENSOR_ANOMALY", "VENTILATION_RECOMMEND");
        assertThat(options.rooms())
                .extracting(AlertHistoryFilterOptionsResponse.RoomOption::roomId, AlertHistoryFilterOptionsResponse.RoomOption::roomName)
                .containsExactly(tuple(7L, "302호"), tuple(9L, "강의실 A"));
    }

    @Test
    void getFilterOptionsReturnsEmptyRoomsWhenCoreUnavailable() {
        when(telegramSubscriptionRepository.findActiveBotTypesByUserId(100L))
                .thenReturn(List.of(BotType.ADMIN_BOT));
        when(alertHistoryRepository.findAlertTypesByUserId(100L))
                .thenReturn(List.of(AlertType.SENSOR_ANOMALY));
        when(coreApiClient.getRoomSubscriptions(100L)).thenThrow(new RuntimeException("core down"));

        AlertHistoryFilterOptionsResponse options = alertHistoryService.getFilterOptions(100L);

        assertThat(options.botTypeList()).containsExactly("ADMIN_BOT");
        assertThat(options.alertTypeList()).containsExactly("SENSOR_ANOMALY");
        assertThat(options.rooms()).isEmpty();
    }

    @Test
    void getFilterOptionsReturnsEmptyRoomsWhenRoomSubInfoNull() {
        when(telegramSubscriptionRepository.findActiveBotTypesByUserId(100L))
                .thenReturn(List.of(BotType.ADMIN_BOT));
        when(alertHistoryRepository.findAlertTypesByUserId(100L))
                .thenReturn(List.of(AlertType.SENSOR_ANOMALY));
        when(coreApiClient.getRoomSubscriptions(100L)).thenReturn(new UserRoomSubResponse(100L, null));

        AlertHistoryFilterOptionsResponse options = alertHistoryService.getFilterOptions(100L);

        assertThat(options.rooms()).isEmpty();
    }
}
