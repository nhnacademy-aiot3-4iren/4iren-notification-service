package com.siren.notificationservice.core.dto.event;

import com.siren.notificationservice.core.entity.domain.AlertType;

import java.time.Instant;
import java.util.List;

public record AlertEvent(
        Long roomId,
        AlertType alertType,
        String alertTitle,
        List<MetricViolationDto> metricViolations,
        Instant detectedAt,
        String eventId // 멱등성 키
) {
    public record MetricViolationDto(
            String deviceEui,
            String metricType, //TEMPERATURE/HUMIDITY/CO2 등
            List<NodeResult> nodeResults

    ){
        public record NodeResult(
           String nodeType,
           Operator operator,// nullable
           String unit,
           Double threshold,
           Double value
        ){}
    }
}
