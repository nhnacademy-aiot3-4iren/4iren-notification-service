package com.siren.notificationservice.core.dto.request;

import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;

import java.time.LocalDate;

// nullable
public record AlertHistorySearchCondition(
        Long roomId,
        BotType botType,
        AlertType alertType,
        LocalDate from,
        LocalDate to
) {
}
