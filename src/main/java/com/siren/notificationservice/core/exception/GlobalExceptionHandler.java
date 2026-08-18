package com.siren.notificationservice.core.exception;

import com.siren.notificationservice.core.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String GENERIC_INTERNAL_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

    private final String activeProfile;

    public GlobalExceptionHandler(@Value("${spring.profiles.active:}") String activeProfile) {
        this.activeProfile = activeProfile;
    }

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

    /**
     * 요청 Role이 허용되지 않을 때 403으로 응답한다.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    /**
     * X-USER-ROLE 헤더가 없거나 알 수 없는 값일 때. 상세는 로깅하고 응답은 제네릭 403으로 내린다.
     */
    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRole(InvalidRoleException e) {
        log.warn("잘못된 Role 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "접근 권한이 없습니다."));
    }

    /**
     * 잘못된 요청 파라미터(예: 알 수 없는 botType/alertType 값으로 enum 변환 실패)일 때 400으로 응답한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청 파라미터: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "잘못된 요청 파라미터입니다."));
    }

    /**
     * dev 프로파일에선 실제 예외 메시지를 그대로 내려서 디버깅에 쓴다.
     * 그 외(prod, 알 수 없는 프로파일 등)는 내부 구현 노출을 막기 위해 항상 제네릭 메시지만 응답한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        String message = "dev".equals(activeProfile)
                ? e.getClass().getSimpleName() + ": " + e.getMessage()
                : GENERIC_INTERNAL_ERROR_MESSAGE;
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), message));
    }
}
