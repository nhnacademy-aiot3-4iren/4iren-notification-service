package com.siren.notificationservice.core.dto.response;

// Core가 기상청 API 응답을 그대로 넘겨주는 원본 형식(KmaCurrentWeatherResponseDto) —
// 숫자에 단위가 붙은 문자열("33.7℃", "69%")이고 baseDateTime도 ISO가 아니라서
// 파싱은 여기서 하지 않고 FeedbackPersistenceService에서 한다. nx/ny는 기상청 격자 좌표로,
// 다중 지역 서비스를 대비해 OutsideWeatherSnapshot의 유니크 키에 그대로 쓰인다.
// wind/regionName 등 나머지 필드는 우리가 안 쓰는 값이라 필드 자체를 안 만들었고,
// Jackson이 알아서 무시한다.
public record OutsideWeather(
        String baseDateTime, // "2026-07-24 13:00" - 실제 데이터 시각, 파싱 전 원본
        String temperature,  // "33.7℃"
        String humidity,     // "69%"
        Integer nx,          // 기상청 격자 좌표 X
        Integer ny           // 기상청 격자 좌표 Y
) {
}