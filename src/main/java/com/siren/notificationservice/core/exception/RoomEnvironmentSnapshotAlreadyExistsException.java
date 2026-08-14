package com.siren.notificationservice.core.exception;

import java.time.LocalDateTime;

public class RoomEnvironmentSnapshotAlreadyExistsException extends RuntimeException {
    public RoomEnvironmentSnapshotAlreadyExistsException(Long roomId, LocalDateTime windowStart) {
        super("roomId=" + roomId + " windowStart=" + windowStart+" already exists");
    }
}
