package com.siren.notificationservice.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 강의실의 특정 15분 구간 환경 스냅샷. 실제 측정값(온도/습도/CO2 등)은
 * {@link RoomEnvironmentReading}에 key-value로 담기고, 이 엔티티는 "언제/어느 강의실"만 식별한다.
 * 같은 강의실의 같은 구간이 중복 저장되지 않도록 (room_id, window_start) 유니크 제약을 둔다.
 */
@Entity
@Table(name = "room_environment_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_room_environment_snapshot_room_window", columnNames = {"room_id", "window_start"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RoomEnvironmentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "window_start", nullable = false)
    private ZonedDateTime windowStart;
}
