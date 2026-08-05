package com.siren.notificationservice.telegram.dto.event;

import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackProcessingEvent(
        Long userId,
        Long roomId,
        String rawText,
        List<FeedbackExtractionResult.SensorScore> sensorScores,
        boolean isDelayed,
        LocalDateTime experiencedAt, // 지연 + 시각 언급 있을 때만 -> 없으면 null
        LocalDateTime receivedAt
) {
}
