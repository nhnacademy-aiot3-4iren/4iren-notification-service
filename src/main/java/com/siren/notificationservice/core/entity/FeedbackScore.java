package com.siren.notificationservice.core.entity;

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

/**
 * {@link FeedbackLog} 한 건에 딸린 축별 체감 점수 하나(key-value, -2~2). 온도/습도/공기질을
 * 포함해 모든 축을 여기서 동일하게 다룬다 — 새 축이 추가돼도 {@link SensorType} 상수만 늘리면 된다.
 */
@Entity
@Table(name = "feedback_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FeedbackScore {

    @EmbeddedId
    private FeedbackScoreId id;

    /**
     * id.feedbackLogId를 이 연관관계에서 파생시킨다 (FK 컬럼과 PK 컬럼이 feedback_log_id로 동일).
     */
    @MapsId("feedbackLogId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_log_id")
    private FeedbackLog feedbackLog;

    @Column(name = "score", nullable = false)
    private Integer score; // -2~2
}
