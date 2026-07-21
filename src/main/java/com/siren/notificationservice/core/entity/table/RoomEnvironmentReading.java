package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * {@link RoomEnvironmentSnapshot} 한 건에 딸린 실측값 하나(key-value). 센서가 없는 강의실은
 * 해당 {@link EnvironmentMetricType}의 row가 그냥 없는 것으로 표현한다 — NULL 값 개념이 필요 없다.
 */
@Entity
@Table(name = "room_environment_reading")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RoomEnvironmentReading {

    @EmbeddedId
    private RoomEnvironmentReadingId id;

    /**
     * id.snapshotId를 이 연관관계에서 파생시킨다 (FK 컬럼과 PK 컬럼이 snapshot_id로 동일).
     */
    @MapsId("snapshotId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private RoomEnvironmentSnapshot snapshot;

    @Column(name = "value", nullable = false, precision = 10, scale = 3)
    private BigDecimal value;
}
