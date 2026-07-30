package com.siren.notificationservice.core.dto.request;

import java.time.LocalDateTime;

public record OutsideWeatherRequest(
        Long roomId,
        LocalDateTime requestDateTime
) {
}
