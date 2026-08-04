package com.siren.notificationservice.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreApiUnavailableExceptionTest {

    @Test
    void messageContainsLabelAndId() {
        CoreApiUnavailableException exception = new CoreApiUnavailableException(5L, "roomId");

        assertThat(exception.getMessage()).contains("roomId=5");
    }
}
