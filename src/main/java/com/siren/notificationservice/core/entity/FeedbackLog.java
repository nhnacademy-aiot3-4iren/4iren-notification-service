package com.siren.notificationservice.core.entity;

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

import java.time.ZonedDateTime;

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

    @Column(name = "avg_temperature", nullable = false)
    private double avgTemperature; // 피드백 직전 15분 실내 평균

    @Column(name = "avg_humidity", nullable = false)
    private double avgHumidity; // 피드백 직전 15분 실내 평균

    @Column(name = "avg_co2")
    private Double avgCo2; // CO2 센서 없는 강의실 대비 NULL 허용

    @Column(name = "temp_score")
    private Integer tempScore; // -2~2, 자연어에서 언급 안 되면 NULL

    @Column(name = "humidity_score")
    private Integer humidityScore; // -2~2, 자연어에서 언급 안 되면 NULL

    @Column(name = "air_quality_score")
    private Integer airQualityScore; // -2~2, 답답함/졸림 등 CO2 체감 축

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText; // 피드백 원문 보존 (재분류/재학습 대비)

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "outside_temperature", nullable = false)
    private double outsideTemperature; // 시점 스냅샷 (평균 아님)

    @Column(name = "outside_humidity")
    private Double outsideHumidity; // 시점 스냅샷

    @Column(name = "outside_condition", length = 10)
    private String outsideCondition; // SUNNY/RAINY/SNOWY/CLOUDY/FOGGY 등

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private NotificationUser notificationUser;
}
