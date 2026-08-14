package com.siren.notificationservice.core.exception;

import java.time.LocalDateTime;

public class OutsideWeatherSnapshotAlreadyExistsException extends RuntimeException {
    public OutsideWeatherSnapshotAlreadyExistsException(Integer nx, Integer ny, LocalDateTime windowStart) {
        super("nx=" + nx + " ny=" + ny + " windowStart=" + windowStart + " already exists");
    }
}
