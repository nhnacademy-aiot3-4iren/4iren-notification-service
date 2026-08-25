package com.siren.notificationservice.telegram.dto.event;

import java.time.LocalDateTime;

public record AccountRoleEvent(
        Long userId,
        String role,
        LocalDateTime updateAt
) {
}
