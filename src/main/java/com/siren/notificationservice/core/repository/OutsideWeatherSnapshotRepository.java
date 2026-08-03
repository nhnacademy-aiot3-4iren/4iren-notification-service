package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface OutsideWeatherSnapshotRepository extends JpaRepository<OutsideWeatherSnapshot, Long> {

    /**
     * 특정 지역(nx, ny)·특정 시간대의 외부 날씨 스냅샷을 조회한다.
     */
    Optional<OutsideWeatherSnapshot> findByNxAndNyAndWindowStart(Integer nx, Integer ny, ZonedDateTime windowStart);
}
