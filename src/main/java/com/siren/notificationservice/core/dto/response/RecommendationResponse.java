package com.siren.notificationservice.core.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long userId,
        Long roomId,
        String message,
        Answer answer,
        LocalDateTime requestedAt,
        LocalDateTime receivedAt,
        LocalDateTime answeredAt
) {
    public record Answer(String answer, List<String> options) {}
}
