package com.siren.notificationservice.core.dto.request;

import com.siren.notificationservice.core.dto.response.RoomSubResponse;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationRequest(
        List<RoomSubResponse> roomSubInfo,
        String message,
        LocalDateTime requestedAt
) {
}
