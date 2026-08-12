package com.siren.notificationservice.core.entity.table;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.ZonedDateTime;
import java.util.List;

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

    @OneToMany(mappedBy = "snapshot", fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<RoomEnvironmentReading> readings;

}
