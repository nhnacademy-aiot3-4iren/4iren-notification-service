package com.siren.notificationservice.core.dto.response;

import java.time.LocalDateTime;

public record RecommendationResponse(
        Long userId,
        Long roomId,
        String question,
        String answer,
        LocalDateTime requestedAt,
        LocalDateTime receivedAt,
        LocalDateTime answeredAt
) {
}
