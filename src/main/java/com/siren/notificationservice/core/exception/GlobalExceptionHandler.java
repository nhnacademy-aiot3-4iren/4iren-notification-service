package com.siren.notificationservice.core.exception;

import com.siren.notificationservice.core.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 컨트롤러(웹훅/딥링크)에서 예상 못 한 예외가 나면 500으로 응답하고 스택트레이스를 로깅한다.
 * 도메인 예외(MissingChatIdException 등)는 전부 RabbitMQ 리스너 안에서만 던져지고 그 안에서
 * 잡히기 때문에 여기까지 올라오지 않는다 -> 그래서 타입별로 나눌 핸들러 없이 이거 하나로 충분하다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다."));
    }
}
