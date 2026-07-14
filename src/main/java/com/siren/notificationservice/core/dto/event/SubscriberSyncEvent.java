package com.siren.notificationservice.core.dto.event;

import com.siren.notificationservice.core.entity.SubscriptionStatus;

import java.time.ZonedDateTime;

public record SubscriberSyncEvent(
        Long userId,
        Long roomId,
        SubscriptionStatus status,
        boolean alarmEnabled,
        ZonedDateTime updatedAt
) {
}
