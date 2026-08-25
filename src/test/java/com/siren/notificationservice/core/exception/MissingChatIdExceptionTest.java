package com.siren.notificationservice.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MissingChatIdExceptionTest {

    @Test
    void messageSaysChatIdMissing() {
        MissingChatIdException exception = new MissingChatIdException();

        assertThat(exception.getMessage()).isEqualTo("chatId가 존재하지 않습니다.");
    }
}
