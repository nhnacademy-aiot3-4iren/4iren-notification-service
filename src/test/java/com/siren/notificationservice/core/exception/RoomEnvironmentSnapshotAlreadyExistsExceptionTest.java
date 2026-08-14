package com.siren.notificationservice.core.exception;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RoomEnvironmentSnapshotAlreadyExistsExceptionTest {

    @Test
    void messageContainsRoomIdAndWindowStart() {
        LocalDateTime windowStart = LocalDateTime.now();

        RoomEnvironmentSnapshotAlreadyExistsException exception =
                new RoomEnvironmentSnapshotAlreadyExistsException(7L, windowStart);

        assertThat(exception.getMessage())
                .contains("roomId=7")
                .contains(windowStart.toString());
    }
}
