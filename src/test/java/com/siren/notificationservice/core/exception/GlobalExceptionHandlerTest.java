package com.siren.notificationservice.core.exception;

import com.siren.notificationservice.core.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void unexpectedExceptionReturns500WithGenericMessageOnProdProfile() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler("prod");

        ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("아무 문제"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }

    @Test
    void unexpectedExceptionReturns500WithGenericMessageWhenProfileUnknown() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler("");

        ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("아무 문제"));

        assertThat(response.getBody().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }

    @Test
    void unexpectedExceptionReturns500WithDetailedMessageOnDevProfile() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler("dev");

        ResponseEntity<ErrorResponse> response = handler.handleException(new NullPointerException("userId는 null일 수 없습니다."));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("NullPointerException: userId는 null일 수 없습니다.");
    }
}
