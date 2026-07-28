package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface OutsideWeatherSnapshotRepository extends JpaRepository<OutsideWeatherSnapshot, Long> {

    /**
     * 특정 시간대(Core가 알려준 실제 날씨 데이터 시각)의 외부 날씨 스냅샷을 조회한다
     * (window_start 유니크 기준 단건). room_id가 없다 — 이 배포가 서비스하는 위치는
     * 하나뿐이라 강의실 구분 없이 시간대로만 재사용한다.
     *
     * @param windowStart 날씨 데이터 시각
     * @return 스냅샷, 없으면 empty
     */
    Optional<OutsideWeatherSnapshot> findByWindowStart(ZonedDateTime windowStart);
}
