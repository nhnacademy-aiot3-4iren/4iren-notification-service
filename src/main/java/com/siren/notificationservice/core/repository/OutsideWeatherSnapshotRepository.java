package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutsideWeatherSnapshotRepository extends JpaRepository<OutsideWeatherSnapshot, Long> {

    /**
     * 특정 지역(nx, ny)·특정 시간대의 외부 날씨 스냅샷을 조회한다.
     */
    Optional<OutsideWeatherSnapshot> findByNxAndNyAndWindowStart(Integer nx, Integer ny, LocalDateTime windowStart);

    @Query(value = "SELECT w.weather_snapshot_id FROM outside_weather_snapshot w " +
            "LEFT JOIN feedback_log fl ON fl.outside_weather_snapshot_id = w.weather_snapshot_id " +
            "WHERE fl.outside_weather_snapshot_id IS NULL LIMIT :batchSize", nativeQuery = true)
    List<Long> findOrphanWeatherSnapshotIds(@Param("batchSize") int batchSize);

    // 조회~삭제 사이 레이스 대비: 삭제 시점에 참조 없는 것만 지운다(0건 삭제는 정상).
    @Modifying
    @Query(value = "DELETE FROM outside_weather_snapshot WHERE weather_snapshot_id IN (:ids) "
            + "AND weather_snapshot_id NOT IN (SELECT fl.outside_weather_snapshot_id FROM feedback_log fl WHERE fl.outside_weather_snapshot_id IS NOT NULL)",
            nativeQuery = true)
    void deleteWeatherSnapshotsByIds(@Param("ids") List<Long> ids);
}
