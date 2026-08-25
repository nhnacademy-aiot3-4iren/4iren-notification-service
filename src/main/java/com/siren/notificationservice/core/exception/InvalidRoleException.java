package com.siren.notificationservice.core.exception;

/**
 * X-USER-ROLE 헤더가 없거나 알 수 없는 값일 때. Gateway 계약 위반 신호.
 */
public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(String message) {
        super(message);
    }
}
