package com.siren.notificationservice.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramSubscriptionNotFoundExceptionTest {

    @Test
    void messageSaysSubscriptionNotFound() {
        TelegramSubscriptionNotFoundException exception = new TelegramSubscriptionNotFoundException();

        assertThat(exception.getMessage()).isEqualTo("해당 chatId로 연동된 텔레그램 구독 정보를 찾을 수 없습니다.");
    }
}
