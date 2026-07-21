package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "4IREN-CORE")
public interface CoreApiClient {

    /**
     * Core API에 이 유저가 현재 구독 중인 강의실 목록을 실시간으로 조회한다.
     * 응답의 각 강의실 항목은 알림 수신 여부(notificationEnabled)도 함께 포함하므로,
     * 조회 범위 판단(수신 여부 무관하게 전체 사용) / 발송 게이트 판단(수신 여부 확인) 양쪽에 재사용 가능하다.
     *
     * @param userId 대상 유저 id
     * @return 유저 id와 구독 중인 강의실별 정보(roomId, notificationEnabled) 목록
     */
    @GetMapping("/api/rooms/subscriptions")
    UserRoomSubResponse getRoomSubscriptions(@RequestParam("userId") Long userId);
}
