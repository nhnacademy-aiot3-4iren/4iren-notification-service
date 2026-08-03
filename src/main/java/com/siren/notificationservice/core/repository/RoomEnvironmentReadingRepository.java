package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomEnvironmentReadingRepository extends JpaRepository<RoomEnvironmentReading, RoomEnvironmentReadingId> {

    /**
     * 특정 스냅샷에 딸린 실측값 전체를 조회한다.
     */
    List<RoomEnvironmentReading> findById_SnapshotId(Long snapshotId);
}
