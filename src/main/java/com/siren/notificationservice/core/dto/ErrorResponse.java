package com.siren.notificationservice.core.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 공통 에러 응답. 예외 타입별 코드 체계 없이 상태/메시지/시각만 담는다.
 */
public record ErrorResponse(
        String message,
        int status,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(message, status, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }
}
