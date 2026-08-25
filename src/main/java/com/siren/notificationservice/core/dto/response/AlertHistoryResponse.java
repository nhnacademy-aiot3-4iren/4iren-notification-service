package com.siren.notificationservice.core.dto.response;

import java.time.LocalDateTime;

public record AlertHistoryResponse(
        Long alertHistoryId,
        Long roomId,
        String botType,
        String alertType,
        String message,
        LocalDateTime sendAt
) {
}
