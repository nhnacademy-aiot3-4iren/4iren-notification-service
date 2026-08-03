package com.siren.notificationservice.core.exception;

import java.time.ZonedDateTime;

public class OutsideWeatherSnapshotAlreadyExistsException extends RuntimeException {
    public OutsideWeatherSnapshotAlreadyExistsException(Integer nx, Integer ny, ZonedDateTime windowStart) {
        super("nx=" + nx + " ny=" + ny + " windowStart=" + windowStart + " already exists");
    }
}
