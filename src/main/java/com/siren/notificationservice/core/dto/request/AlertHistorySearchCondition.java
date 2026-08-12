package com.siren.notificationservice.core.dto.request;

import java.time.LocalDate;

// nullable
public record AlertHistorySearchCondition(
        Long roomId,
        String botType,
        String alertType,
        LocalDate from,
        LocalDate to
) {
}
