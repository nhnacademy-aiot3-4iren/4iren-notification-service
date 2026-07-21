package com.siren.notificationservice.core.exception;

import lombok.Getter;

@Getter
public abstract class NotificationServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode에 정의된 기본 메시지로 예외를 생성한다.
     *
     * @param errorCode 에러 코드
     */
    protected NotificationServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 상세 컨텍스트(예: 대상 id)를 메시지에 포함해 예외를 생성한다.
     *
     * @param errorCode 에러 코드
     * @param detail    상세 메시지
     */
    protected NotificationServiceException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
