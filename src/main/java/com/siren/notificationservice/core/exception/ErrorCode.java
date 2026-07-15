package com.siren.notificationservice.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    NOTIFICATION_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_USER_NOT_FOUND", "유저 동기화 정보를 찾을 수 없습니다."),
    TOKEN_EXPIRED(HttpStatus.GONE, "TOKEN_EXPIRED", "만료되었거나 존재하지 않는 토큰입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
