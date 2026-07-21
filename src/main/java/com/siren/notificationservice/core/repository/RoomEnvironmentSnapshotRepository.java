package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.RoomEnvironmentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface RoomEnvironmentSnapshotRepository extends JpaRepository<RoomEnvironmentSnapshot, Long> {

    /**
     * 특정 강의실의 특정 15분 구간 스냅샷을 조회한다 (room_id, window_start 유니크 기준 단건).
     *
     * @param roomId      대상 강의실 id
     * @param windowStart 구간 시작 시각
     * @return 스냅샷, 없으면 empty
     */
    Optional<RoomEnvironmentSnapshot> findByRoomIdAndWindowStart(Long roomId, ZonedDateTime windowStart);
}
