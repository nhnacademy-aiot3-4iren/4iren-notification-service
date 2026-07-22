package com.siren.notificationservice.core.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationRequest(
        Long lastMentionRoomId,
        List<Long> subscribedRoomIds,
        String question,
        LocalDateTime requestedAt
) {
}
