package com.siren.notificationservice.core.exception;

/**
 * Account가 발행한 role 동기화 이벤트가 처리 불가능한 형태(필드 누락, 알 수 없는 role 값 등)일 때 발생한다.
 * 재시도해도 결과가 달라지지 않는 실패라 DLQ로 보내 관리자가 직접 확인하게 한다.
 */
public class InvalidAccountRoleEventException extends RuntimeException {

    public InvalidAccountRoleEventException(String message) {
        super(message);
    }

    public InvalidAccountRoleEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
