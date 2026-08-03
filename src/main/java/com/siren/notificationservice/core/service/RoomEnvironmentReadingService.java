package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.repository.RoomEnvironmentReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * RoomEnvironmentReading에 대한 순수 DB 접근 계층. Core 응답을 엔티티로 매핑하는 로직(metricType
 * 매핑 등)은 이 클래스가 아니라 호출하는 쪽(RoomEnvironmentSnapshotService 등)의 책임이다.
 */
@Service
@RequiredArgsConstructor
public class RoomEnvironmentReadingService {
    private final RoomEnvironmentReadingRepository roomEnvironmentReadingRepository;

    /**
     * 특정 스냅샷에 딸린 실측값 전체를 조회한다.
     */
    @Transactional(readOnly = true)
    public List<RoomEnvironmentReading> getReadingsBySnapshotId(Long snapshotId) {
        Objects.requireNonNull(snapshotId, "snapshotId는 null일 수 없습니다.");
        return roomEnvironmentReadingRepository.findById_SnapshotId(snapshotId);
    }

    /**
     * 실측값들을 한 번에 저장한다. REQUIRED라 호출부(createWithReadings)의
     * REQUIRES_NEW 트랜잭션에 합류한다.
     */
    @Transactional
    public List<RoomEnvironmentReading> saveAll(List<RoomEnvironmentReading> readings) {
        Objects.requireNonNull(readings, "readings는 null일 수 없습니다.");
        return roomEnvironmentReadingRepository.saveAll(readings);
    }
}
