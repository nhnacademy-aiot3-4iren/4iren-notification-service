package com.siren.notificationservice.telegram.exception;

import com.siren.notificationservice.core.exception.ErrorCode;
import com.siren.notificationservice.core.exception.NotificationServiceException;

public class TokenExpiredException extends NotificationServiceException {

    /**
     * @param message 상세 메시지 (예: 어떤 토큰이 만료/누락됐는지)
     */
    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }
}
