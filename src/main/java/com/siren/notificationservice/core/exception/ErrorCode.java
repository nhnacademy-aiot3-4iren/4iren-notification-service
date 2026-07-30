package com.siren.notificationservice.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    CORE_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CORE_API_UNAVAILABLE", "지금은 확인이 어려워요, 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    MISSING_CHAT_ID(HttpStatus.BAD_REQUEST, "MISSING_CHAT_ID", "chatId가 존재하지 않습니다."),
    TELEGRAM_SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "TELEGRAM_SUBSCRIPTION_NOT_FOUND", "해당 chatId로 연동된 텔레그램 구독 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
