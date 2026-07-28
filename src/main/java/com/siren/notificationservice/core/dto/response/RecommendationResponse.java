package com.siren.notificationservice.core.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long userId,
        Long roomId,
        String message,
        String answer,
        List<String> options,
        LocalDateTime requestedAt,
        LocalDateTime receivedAt,
        LocalDateTime answeredAt
) {
}
