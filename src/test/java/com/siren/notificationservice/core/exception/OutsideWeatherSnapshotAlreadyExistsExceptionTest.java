package com.siren.notificationservice.core.exception;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutsideWeatherSnapshotAlreadyExistsExceptionTest {

    @Test
    void messageContainsNxNyAndWindowStart() {
        LocalDateTime windowStart = LocalDateTime.now();

        OutsideWeatherSnapshotAlreadyExistsException exception =
                new OutsideWeatherSnapshotAlreadyExistsException(60, 127, windowStart);

        assertThat(exception.getMessage())
                .contains("nx=60")
                .contains("ny=127")
                .contains(windowStart.toString());
    }
}
