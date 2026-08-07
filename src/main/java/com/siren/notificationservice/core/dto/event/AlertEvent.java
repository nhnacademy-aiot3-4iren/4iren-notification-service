package com.siren.notificationservice.core.dto.event;

import com.siren.notificationservice.core.entity.domain.AlertType;

import java.time.Instant;
import java.util.List;

public record AlertEvent(
        Long roomId,
        AlertType alertType,
        String alertTitle,
        String deviceEui,
        String deviceName,
        String point,
        List<NodeResult> nodeResults,
        Instant detectedAt,
        String eventId // 멱등성 키
) {
    public record NodeResult(
            String nodeType,
            String metricType, //TEMPERATURE/HUMIDITY/CO2 등
            Operator operator, // nullable
            String unit,
            Double threshold,
            Double value
    ) {
    }
}
