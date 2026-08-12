package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = "SELECT s.snapshot_id FROM room_environment_snapshot s " +
            "LEFT JOIN feedback_log fl ON fl.snapshot_id = s.snapshot_id " +
            "WHERE fl.snapshot_id IS NULL LIMIT :batchSize", nativeQuery = true)
    List<Long> findOrphanSnapshotIds(@Param("batchSize") int batchSize);

    @Modifying
    @Query(value = "DELETE FROM room_environment_snapshot WHERE snapshot_id IN  (:ids)", nativeQuery = true)
    void deleteSnapshotsByIds(@Param("ids") List<Long> ids);
}
