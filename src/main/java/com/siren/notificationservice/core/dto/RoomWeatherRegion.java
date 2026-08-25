package com.siren.notificationservice.core.dto;

/**
 * roomId 하나에 대응하는 기상청 격자 좌표. Redis에 TTL 캐싱해서 Core 호출 여부 판단에 쓴다.
 */
public record RoomWeatherRegion(Integer nx, Integer ny) {
}
