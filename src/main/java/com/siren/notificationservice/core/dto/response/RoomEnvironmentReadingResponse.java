package com.siren.notificationservice.core.dto.response;

import java.time.Instant;
import java.util.List;

public record RoomEnvironmentReadingResponse(
    Long roomId,
    Instant calculatedAt,
    List<MetricAverage> metrics
) {
    public record MetricAverage(
            String metricType, // Core API 원본 값("temperature", "humidity", "co2" - 소문자) - 매핑 시 toUpperCase() 필요
            Double averageValue
    ){}
}
