package com.siren.notificationservice.core.exception;

import com.siren.notificationservice.core.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인에서 의도적으로 던진 예외(NotificationServiceException 계열)를 ErrorCode에 매핑된
     * HTTP 상태/메시지로 변환한다.
     *
     * @param e 도메인 예외
     * @return 에러 코드에 대응하는 상태의 ErrorResponse
     */
    @ExceptionHandler(NotificationServiceException.class)
    public ResponseEntity<ErrorResponse> handleNotificationServiceException(NotificationServiceException e) {
        log.warn("[{}] {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 예상하지 못한 나머지 모든 예외에 대한 최종 폴백. 500으로 응답하고 스택트레이스를 로깅한다.
     *
     * @param e 처리되지 않은 예외
     * @return INTERNAL_SERVER_ERROR ErrorResponse
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
