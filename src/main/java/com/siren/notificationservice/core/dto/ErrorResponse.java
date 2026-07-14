package com.siren.notificationservice.core.dto;

import com.siren.notificationservice.core.exception.ErrorCode;

import java.time.ZonedDateTime;

public record ErrorResponse(
        String code,
        String message,
        int status,
        ZonedDateTime timestamp
) {

    /**
     * ErrorCode의 기본 메시지로 응답을 만든다.
     *
     * @param errorCode 에러 코드
     * @return 에러 응답
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errorCode.getStatus().value(), ZonedDateTime.now());
    }

    /**
     * 메시지를 상세 컨텍스트로 덮어써서 응답을 만든다.
     *
     * @param errorCode 에러 코드
     * @param message   상세 메시지
     * @return 에러 응답
     */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, errorCode.getStatus().value(), ZonedDateTime.now());
    }
}
