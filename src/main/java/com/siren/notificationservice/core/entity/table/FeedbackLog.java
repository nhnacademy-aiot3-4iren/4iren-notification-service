package com.siren.notificationservice.core.entity.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * 사용자가 텔레그램으로 남긴 강의실 환경 체감 피드백 한 건. 축별 점수는
 * {@link FeedbackScore}(key-value)로, 실내 환경 실측값은 {@link RoomEnvironmentSnapshot}/
 * {@link RoomEnvironmentReading}으로 분리돼 있다 — 이 엔티티는 원문/제출 메타데이터/외부 날씨만 담는다.
 */
@Entity
@Table(name = "feedback_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FeedbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackLogId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /**
     * 피드백 시점(정확히는 experiencedAt 또는 createdAt)에 해당하는 환경 스냅샷.
     * 센서가 없는 강의실이거나 매칭되는 구간이 없으면 NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private RoomEnvironmentSnapshot snapshot;

    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    private String rawText; // 피드백 원문 보존 (재분류/재학습 대비), 자연어 전용이라 항상 존재

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt; // 피드백 제출 시각

    @Column(name = "is_delayed", nullable = false)
    private boolean delayed; // 자연어에서 지연 제출 신호("아까", "집에 와서" 등)가 감지되면 true

    @Column(name = "experienced_at")
    private ZonedDateTime experiencedAt; // 사용자가 언급한 구체적 체감 시각(있으면 환경 스냅샷 매칭 기준점), 없으면 NULL

    @Column(name = "outside_temperature", nullable = false, precision = 4, scale = 1)
    private BigDecimal outsideTemperature; // 시점 스냅샷 (평균 아님)

    @Column(name = "outside_humidity", precision = 4, scale = 1)
    private BigDecimal outsideHumidity; // 시점 스냅샷

    @Column(name = "outside_condition", length = 10)
    private String outsideCondition; // SUNNY/RAINY/SNOWY/CLOUDY/FOGGY 등

    @Column(name = "user_id", nullable = false)
    private Long userId; // Account API 소유 유저 id (bare, 로컬 FK 없음)
}
