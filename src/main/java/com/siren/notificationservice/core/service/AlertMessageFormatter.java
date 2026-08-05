package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.dto.event.Operator;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AlertEvent를 텔레그램 메시지 본문으로 변환한다.
 */
@Component
public class AlertMessageFormatter {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DETECTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 알림 하나를 사람이 읽을 메시지 본문으로 조립한다.
     */
    public String format(AlertEvent event, String roomName) {
        StringBuilder message = new StringBuilder();
        message.append(event.alertType().isUrgent() ? "🚨 [긴급] " : "📋 ") // 긴급/비긴급인지
                .append(event.alertTitle()) //event로 날아온 알림 타이틀
                .append("\n📍 ").append(roomName).append(" · ") // 룸 이름
                .append(ZonedDateTime.ofInstant(event.detectedAt(), SERVICE_ZONE).format(DETECTED_AT_FORMAT)) //시각
                .append("\n\n");

        for (AlertEvent.MetricViolationDto violation : event.metricViolations()) { // 실제 내용
            message.append("⚠️ ")
                    .append(violation.metricType()) //온도인지 습도인지 co2인지 등등
                    .append(" ").append(violation.value()) // 값
                    .append(" — 기준 ").append(Operator.labelOf(violation.operator()))
                    .append("\n");
        }

        return message.toString();
    }
}
