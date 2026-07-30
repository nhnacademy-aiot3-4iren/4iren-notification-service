package com.siren.notificationservice.core.dto.request;

import java.time.LocalDateTime;

public record RoomSensorsReadingRequest(
        Long roomId,
        LocalDateTime requestDateTime
) {
}
