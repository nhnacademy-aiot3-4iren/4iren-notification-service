package com.siren.notificationservice.core.dto;

import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;

import java.util.List;

public record PendingUserReply(
        String rawText,
        List<RoomCandidate> candidates,
        FeedbackExtractionResult feedbackExtractionResult
) {
    public record RoomCandidate(Long roomId, String roomName) {}
}
