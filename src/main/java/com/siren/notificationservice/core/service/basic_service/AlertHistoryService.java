package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.AlertHistoryKey;
import com.siren.notificationservice.core.dto.request.AlertHistorySearchCondition;
import com.siren.notificationservice.core.dto.response.AlertHistoryFilterOptionsResponse;
import com.siren.notificationservice.core.dto.response.AlertHistoryResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import com.siren.notificationservice.core.exception.NotFoundAlertHistoryException;
import com.siren.notificationservice.core.repository.AlertHistoryRepository;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertHistoryService {
    private static final String USER_ID_NULL_MESSAGE = "userId는 null일 수 없습니다.";
    private static final String ALERT_HISTORY_ID_NULL_MESSAGE = "alertHistoryId는 null일 수 없습니다.";
    private static final String EVENT_ID_NULL_MESSAGE = "eventId는 null일 수 없습니다.";

    private final AlertHistoryRepository alertHistoryRepository;
    private final TelegramSubscriptionRepository telegramSubscriptionRepository;
    private final CoreApiClient coreApiClient;

    /**
     * 이 유저에게 발송된 알림 이력을 조회한다. 한 룸에 관리자가 여러 명이어도 각자 자기한테 온 것만 본다.
     */
    @Transactional(readOnly = true)
    public Page<AlertHistoryResponse> getAlertHistoryByUserId(Long userId, AlertHistorySearchCondition filter, Pageable pageable) {
        Objects.requireNonNull(userId, USER_ID_NULL_MESSAGE);
        return alertHistoryRepository.search(userId,filter, pageable).map(this::toResponse);
    }

    /**
     * 알림 이력 단건을 조회한다. 요청자(userId) 소유가 아니면 존재 여부를 노출하지 않도록 not-found로 처리한다.
     */
    @Transactional(readOnly = true)
    public AlertHistoryResponse getAlertHistoryById(Long alertHistoryId, Long userId) {
        Objects.requireNonNull(alertHistoryId, ALERT_HISTORY_ID_NULL_MESSAGE);
        Objects.requireNonNull(userId, USER_ID_NULL_MESSAGE);
        return alertHistoryRepository.findByAlertHistoryIdAndUserId(alertHistoryId, userId).map(this::toResponse)
                .orElseThrow(() -> new NotFoundAlertHistoryException(alertHistoryId));
    }

    /**
     * 이 이벤트가 해당 유저의 해당 봇에 이미 발송됐는 지 확인한다
     * @param eventId 들어온 이벤트 키
     * @return AlertHistoryKey set
     */
    @Transactional(readOnly = true)
    public Set<AlertHistoryKey> findAlreadySentKeys(String eventId){
        Objects.requireNonNull(eventId, EVENT_ID_NULL_MESSAGE);

        return alertHistoryRepository.findByEventId(eventId).stream()
                .map(h -> new AlertHistoryKey(h.getUserId(), h.getBotType()))
                .collect(Collectors.toSet());
    }

    /**
     * 발송 이력을 배치 처리한다.
     * @param alertHistories 저장할 발송 이력 리스트
     */
    @Transactional
    public void saveAll(List<AlertHistory> alertHistories) {
        alertHistoryRepository.saveAll(alertHistories);
    }

    private AlertHistoryResponse toResponse(AlertHistory alertHistory) {
        return new AlertHistoryResponse(
                alertHistory.getAlertHistoryId(),
                alertHistory.getRoomId(),
                alertHistory.getBotType().name(),
                alertHistory.getAlertType() != null ? alertHistory.getAlertType().name() : null,
                alertHistory.getMessage(),
                alertHistory.getSendAt()
        );
    }

    @Transactional(readOnly = true)
    public AlertHistoryFilterOptionsResponse getFilterOptions(Long userId){
        Objects.requireNonNull(userId, USER_ID_NULL_MESSAGE);
        List<String> botTypes = telegramSubscriptionRepository.findActiveBotTypesByUserId(userId)
                .stream()
                .map(Enum::name)
                .toList();
        List<String> alertTypes = alertHistoryRepository.findAlertTypesByUserId(userId)
                .stream()
                .map(Enum::name)
                .toList();

        return new AlertHistoryFilterOptionsResponse(
                botTypes,
                alertTypes,
                resolveSubscribedRooms(userId)
        );
    }

    /**
     * 이 유저가 구독한 강의실(id+이름)을 방 필터 옵션으로 조회한다. Core 실패 시 방 목록만 비운다.
     */
    private List<AlertHistoryFilterOptionsResponse.RoomOption> resolveSubscribedRooms(Long userId) {
        try {
            UserRoomSubResponse subs = coreApiClient.getRoomSubscriptions(userId);
            if (subs == null || subs.roomSubInfo() == null) {
                return List.of();
            }
            return subs.roomSubInfo().stream()
                    .map(r -> new AlertHistoryFilterOptionsResponse.RoomOption(r.roomId(), r.roomName()))
                    .toList();
        } catch (Exception e) {
            log.warn("구독 강의실 조회 실패 - 방 필터 옵션 생략 userId={}", userId, e);
            return List.of();
        }
    }


}
