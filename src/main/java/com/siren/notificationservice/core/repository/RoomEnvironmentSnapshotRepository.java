package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomEnvironmentSnapshotRepository extends JpaRepository<RoomEnvironmentSnapshot, Long> {

    /**
     * referenceAt을 커버 구간([windowStart-15분, windowStart])에 포함하는 스냅샷을 찾는다.
     */
    Optional<RoomEnvironmentSnapshot> findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(
            Long roomId, ZonedDateTime referenceAt, ZonedDateTime referenceAtPlus15);

    /**
     * 특정 강의실의 스냅샷 전체를 조회한다.
     */
    List<RoomEnvironmentSnapshot> findByRoomId(Long roomId);
}
