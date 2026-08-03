package com.siren.notificationservice.core.exception;

import java.time.ZonedDateTime;

public class RoomEnvironmentSnapshotAlreadyExistsException extends RuntimeException {
    public RoomEnvironmentSnapshotAlreadyExistsException(Long roomId, ZonedDateTime windowStart) {
        super("roomId=" + roomId + " windowStart=" + windowStart+" already exists");
    }
}
