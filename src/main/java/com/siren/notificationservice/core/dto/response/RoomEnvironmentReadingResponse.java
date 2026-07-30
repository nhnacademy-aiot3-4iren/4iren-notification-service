package com.siren.notificationservice.core.dto.response;

import java.util.List;

public record RoomEnvironmentReadingResponse(
    Long roomId,
    List<MetricReading> readings
) {
    public record MetricReading(
            String metricType, // Core API 원본 값("TEMPERATURE", "HUMIDITY", "CO2") - 아직 notifi쪽 enum과 매핑전
            Double value
    ){}
}
