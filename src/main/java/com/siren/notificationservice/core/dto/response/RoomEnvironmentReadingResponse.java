package com.siren.notificationservice.core.dto.response;

import java.util.List;

public record RoomEnvironmentReadingResponse(
    Long roomId,
    List<MetricReading> readings,
    OutsideWeather outsideWeather // 날씨 API도 실패할 수 있으니 nullable
) {
    public record MetricReading(
            String metricType, // Core API 원본 값("TEMPERATURE", "HUMIDITY", "CO2") - 아직 notifi쪽 enum과 매핑전
            Double value
    ){}

    // Core가 기상청 API 응답을 그대로 넘겨주는 원본 형식(KmaCurrentWeatherResponseDto) —
    // 숫자에 단위가 붙은 문자열("33.7℃", "69%")이고 baseDateTime도 ISO가 아니라서
    // 파싱은 여기서 하지 않고 FeedbackPersistenceService에서 한다. wind/nx/ny/regionName 등
    // Core 응답의 나머지 필드는 우리가 안 쓰는 값이라 필드 자체를 안 만들었고, Jackson이
    // 알아서 무시한다.
    public record OutsideWeather(
            String baseDateTime, // "2026-07-24 13:00" - 실제 데이터 시각, 파싱 전 원본
            String temperature,  // "33.7℃"
            String humidity      // "69%"
    ){}
}
