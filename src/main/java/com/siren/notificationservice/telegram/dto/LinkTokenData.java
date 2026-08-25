package com.siren.notificationservice.telegram.dto;

import com.siren.notificationservice.core.entity.domain.UserRole;

public record LinkTokenData(
        Long userId,
        UserRole role
) {
}
