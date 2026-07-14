package com.siren.notificationservice.core.dto.event;

import java.time.ZonedDateTime;

public record UserSyncEvent(
        Long userId,
        String userName,
        boolean active,
        ZonedDateTime syncedAt // Account API 쪽에서 이 상태가 확정된 시각
) {
}
