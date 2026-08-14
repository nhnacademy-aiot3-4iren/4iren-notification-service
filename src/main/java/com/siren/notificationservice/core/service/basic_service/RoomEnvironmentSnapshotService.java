package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.exception.RoomEnvironmentSnapshotAlreadyExistsException;
import com.siren.notificationservice.core.repository.RoomEnvironmentSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * RoomEnvironmentSnapshot에 대한 순수 DB 접근 계층. windowStart는 고정 격자가 아니라
 * Core에 실제로 물어본 정확한 시각이고, 커버 구간은 [windowStart-15분, windowStart]다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomEnvironmentSnapshotService {
    private final RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository;
    private final RoomEnvironmentReadingService roomEnvironmentReadingService;

    /**
     * snapshotId로 스냅샷 단건을 조회한다.
     */
    public RoomEnvironmentSnapshot getRoomEnvironmentSnapshot(Long snapshotId) {
        Objects.requireNonNull(snapshotId, "snapshotId는 null일 수 없습니다.");
        return roomEnvironmentSnapshotRepository.findById(snapshotId).orElse(null);
    }

    /**
     * 특정 강의실의 스냅샷 전체를 조회한다.
     */
    public List<RoomEnvironmentSnapshot> getRoomEnvironmentSnapshotByRoomId(Long roomId) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        return roomEnvironmentSnapshotRepository.findByRoomId(roomId);
    }

    /**
     * referenceAt을 커버하는 스냅샷이 있으면 그 id를, 없으면 null을 반환한다.
     */
    public RoomEnvironmentSnapshot findSnapshotId(Long roomId, LocalDateTime referenceAt) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        Objects.requireNonNull(referenceAt, "referenceAt은 null일 수 없습니다.");
        return roomEnvironmentSnapshotRepository
                .findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(roomId, referenceAt, referenceAt.plusMinutes(15))
                .orElse(null);
    }

    /**
     * snapshotId로 프록시 참조만 만든다 (DB 조회 없음).
     */
    public RoomEnvironmentSnapshot getReferenceById(Long snapshotId) {
        Objects.requireNonNull(snapshotId, "snapshotId는 null일 수 없습니다.");
        return roomEnvironmentSnapshotRepository.getReferenceById(snapshotId);
    }

    /**
     * Snapshot과 Reading들을 하나의 트랜잭션으로 저장한다(둘이 원자적으로 묶여야 함).
     */
    @Transactional
    public RoomEnvironmentSnapshot createWithReadings(Long roomId, LocalDateTime referenceAt, RoomEnvironmentReadingResponse readings) {
        Objects.requireNonNull(readings, "readings는 null일 수 없습니다.");
        RoomEnvironmentSnapshot snapshot = createRoomEnvironmentSnapshot(roomId, referenceAt);

        List<RoomEnvironmentReading> validReadings = readings.metrics().stream()
                .map(reading -> toReadingOrNull(roomId, snapshot, reading))
                .filter(Objects::nonNull)
                .toList();
        roomEnvironmentReadingService.saveAll(validReadings);
        return snapshot;
    }

    /**
     * 강의실의 새 스냅샷을 만든다. 같은 referenceAt이 이미 있으면 예외를 던진다.
     */
    private RoomEnvironmentSnapshot createRoomEnvironmentSnapshot(Long roomId, LocalDateTime referenceAt) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        Objects.requireNonNull(referenceAt, "referenceAt은 null일 수 없습니다.");
        if (findSnapshotId(roomId, referenceAt) != null) {
            throw new RoomEnvironmentSnapshotAlreadyExistsException(roomId, referenceAt);
        }
        return roomEnvironmentSnapshotRepository.save(
                RoomEnvironmentSnapshot.builder()
                        .roomId(roomId)
                        .windowStart(referenceAt)
                        .build()
        );
    }

    /**
     * Core 응답의 metricType 문자열을 enum으로 매핑한다. 모르는 값이면 이 reading만 건너뛴다.
     */
    private RoomEnvironmentReading toReadingOrNull(Long roomId, RoomEnvironmentSnapshot snapshot,
                                                    RoomEnvironmentReadingResponse.MetricAverage reading) {
        try {
            EnvironmentMetricType metricType = EnvironmentMetricType.valueOf(reading.metricType().toUpperCase());

            // snapshotId는 @MapsId("snapshotId")가 snapshot 연관관계에서 값을 가져와 채워줌!
            RoomEnvironmentReadingId id = RoomEnvironmentReadingId.builder()
                    .metricType(metricType)
                    .build();

            return RoomEnvironmentReading.builder()
                    .id(id)
                    .snapshot(snapshot)
                    .value(BigDecimal.valueOf(reading.averageValue())) // DTO는 Double, 엔티티 컬럼은 DECIMAL
                    .build();
        } catch (IllegalArgumentException e) {
            // Core가 활동성/조도 등 우리가 안 쓰는 센서도 같이 보내는 게 정상이라(CLAUDE.md 원칙:
            // 비체감 피처는 AI Predict API 영역), 매번 찍히는 게 예상된 상황이라 WARN이 아니라 DEBUG로.
            log.debug("[RoomEnvironmentSnapshotService] 알 수 없는(또는 사용 범위 밖) metricType, 이 reading 건너뜀 (roomId={}, metricType={})", roomId, reading.metricType());
            return null;
        }
    }
}
