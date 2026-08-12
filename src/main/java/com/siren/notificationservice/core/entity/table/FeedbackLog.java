package com.siren.notificationservice.core.entity.table;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 사용자가 텔레그램으로 남긴 강의실 환경 체감 피드백 한 건. 축별 점수는
 * {@link FeedbackScore}(key-averageValue)로, 실내 환경 실측값은 {@link RoomEnvironmentSnapshot}/
 * {@link RoomEnvironmentReading}으로, 외부 날씨는 {@link OutsideWeatherSnapshot}으로
 * 분리돼 있다 — 이 엔티티는 원문/제출 메타데이터만 담는다.
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
    
    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    private String rawText; // 피드백 원문 보존 (재분류/재학습 대비), 자연어 전용이라 항상 존재

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt; // 피드백 제출 시각

    @Column(name = "is_delayed", nullable = false)
    private boolean delayed; // 자연어에서 지연 제출 신호("아까", "집에 와서" 등)가 감지되면 true

    @Column(name = "experienced_at")
    private ZonedDateTime experiencedAt; // 사용자가 언급한 구체적 체감 시각(있으면 환경 스냅샷 매칭 기준점), 없으면 NULL

    @Column(name = "user_id", nullable = false)
    private Long userId; // Account API 소유 유저 id (bare, 로컬 FK 없음)

    /**
     * 피드백 시점(정확히는 experiencedAt 또는 createdAt)에 해당하는 환경 스냅샷.
     * 센서가 없는 강의실이거나 매칭되는 구간이 없으면 NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private RoomEnvironmentSnapshot snapshot;

    /**
     * 피드백 시점(정확히는 Core가 알려준 실제 날씨 데이터 시각) 기준 외부 날씨 스냅샷.
     * 날씨 API 실패 시 NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outside_weather_snapshot_id")
    private OutsideWeatherSnapshot outsideWeatherSnapshot;

    @OneToMany(mappedBy = "feedbackLog", fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<FeedbackScore> scores;


}
