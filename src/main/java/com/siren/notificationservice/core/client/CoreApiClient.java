package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.response.OutsideWeather;
import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.dto.response.RoomSubscribersResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@FeignClient(name = "4IREN-CORE", contextId = "coreApi", fallback = CoreApiClientFallback.class)
public interface CoreApiClient {

    /**
     * Core API에 이 유저가 현재 구독 중인 강의실 목록을 실시간으로 조회한다.
     * 응답의 각 강의실 항목은 알림 수신 여부(notificationEnabled)도 함께 포함하므로,
     * 조회 범위 판단(수신 여부 무관하게 전체 사용) / 발송 게이트 판단(수신 여부 확인) 양쪽에 재사용 가능하다.
     *
     * @param userId 대상 유저 id
     * @return 유저 id와 구독 중인 강의실별 정보(roomId, notificationEnabled) 목록
     */
    @GetMapping("/api/core/internal/users/{user-id}/room-subscriptions")
    UserRoomSubResponse getRoomSubscriptions(@PathVariable("user-id") Long userId);

    @GetMapping("/api/core/internal/rooms/{room-id}/subscribers")
    RoomSubscribersResponse getSubscribers(@PathVariable("room-id") Long roomId);

    /**
     * 해당 강의실에 대한 환경 값 스냡샷을 남긴다.
     */
    @PostMapping("/api/core/internal/rooms/{room-id}/sensor-metrics/summary")
    RoomEnvironmentReadingResponse getRoomSensorsReadings(@PathVariable("room-id")Long roomId, @RequestParam LocalDateTime requestAt);

    /**
     * 해당 강의실에 대한 외부날씨 스냅샷을 남긴다.
     */
    @GetMapping("/api/core/kma/rooms/{room-id}/weather")
    OutsideWeather getOutsideWeather(@PathVariable("room-id")Long roomId, @RequestParam("dateTime") LocalDateTime requestAt);

}
