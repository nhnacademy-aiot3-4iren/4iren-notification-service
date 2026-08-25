package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_alert_history_event_user_bot", columnNames = {"event_id", "user_id", "bot_type"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertHistoryId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_type", length = 20, nullable = false)
    private BotType botType;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 50)
    private AlertType alertType; // 예: VENTILATION_RECOMMEND, SENSOR_ANOMALY

    @Column(name = "event_id", length = 100, nullable = false)
    private String eventId; // Processing/RuleEngine이 발급 -> (event_id, user_id) 복합 UNIQUE로 재처리 시 중복 발송 방지

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "send_at", nullable = false)
    private LocalDateTime sendAt;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Account API 소유 유저 id (bare, 로컬 FK 없음)
}
