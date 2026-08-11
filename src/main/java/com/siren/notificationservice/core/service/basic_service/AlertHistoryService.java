package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.dto.AlertHistoryKey;
import com.siren.notificationservice.core.dto.response.AlertHistoryResponse;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import com.siren.notificationservice.core.exception.NotFoundAlertHistoryException;
import com.siren.notificationservice.core.repository.AlertHistoryRepository;
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
    private final AlertHistoryRepository alertHistoryRepository;

    /**
     * 이 유저에게 발송된 알림 이력을 조회한다. 한 룸에 관리자가 여러 명이어도 각자 자기한테 온 것만 본다.
     */
    @Transactional(readOnly = true)
    public Page<AlertHistoryResponse> getAlertHistoryByUserId(Long userId, Pageable pageable) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        return alertHistoryRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    /**
     * 알림 이력 단건을 조회한다. 요청자(userId) 소유가 아니면 존재 여부를 노출하지 않도록 not-found로 처리한다.
     */
    @Transactional(readOnly = true)
    public AlertHistoryResponse getAlertHistoryById(Long alertHistoryId, Long userId) {
        Objects.requireNonNull(alertHistoryId, "alertHistoryId는 null일 수 없습니다.");
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
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
        Objects.requireNonNull(eventId, "eventId는 null일 수 없습니다.");

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
                alertHistory.getSendAt().toLocalDateTime()
        );
    }


}
