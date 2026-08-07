package com.siren.notificationservice.core.dto.event;

public record AlertDigestBufferEntry(
        AlertEvent event,
        String roomName
) {
}
