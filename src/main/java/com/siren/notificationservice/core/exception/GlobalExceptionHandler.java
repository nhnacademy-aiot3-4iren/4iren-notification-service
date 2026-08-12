package com.siren.notificationservice.core.exception;

import com.siren.notificationservice.core.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 컨트롤러에서 올라오는 예외를 상태 코드로 매핑한다. 타입별 핸들러가 없으면 catch-all이
 * 500으로 응답하고 스택트레이스를 로깅한다. (웹훅/딥링크의 도메인 예외는 대부분 RabbitMQ
 * 리스너 안에서 잡혀 여기까지 올라오지 않는다.)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 조회 대상이 없거나 요청자 소유가 아닐 때. 존재 여부를 노출하지 않도록 404로 응답한다.
     */
    @ExceptionHandler(NotFoundAlertHistoryException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundAlertHistoryException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    /**
     * X-USER-ID 등 필수 요청 헤더가 빠졌을 때 400으로 응답한다.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                        "필수 요청 헤더가 없습니다: " + e.getHeaderName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다."));
    }
}
