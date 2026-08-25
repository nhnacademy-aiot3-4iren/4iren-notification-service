package com.siren.notificationservice.core.dto.response;

import java.util.List;

public record RoomSubscribersResponse(
        Long roomId,
        String roomName,
        List<SubscribersResponse> subscribers
) {
    public record SubscribersResponse(
        Long userId,
        boolean notificationEnabled
    ) {
    }
}
