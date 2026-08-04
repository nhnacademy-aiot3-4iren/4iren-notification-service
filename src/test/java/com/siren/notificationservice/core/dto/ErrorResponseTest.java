package com.siren.notificationservice.core.dto;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void ofSetsStatusAndMessageWithCurrentTimestamp() {
        ZonedDateTime before = ZonedDateTime.now();

        ErrorResponse response = ErrorResponse.of(400, "잘못된 요청입니다");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("잘못된 요청입니다");
        assertThat(response.timestamp()).isAfterOrEqualTo(before);
    }
}
