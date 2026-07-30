package com.siren.notificationservice.core.exception;

public class CoreApiUnavailableException extends NotificationServiceException {

    public CoreApiUnavailableException(Long id, String message) {
        super(ErrorCode.CORE_API_UNAVAILABLE, "Core API 응답 실패 ("+message+"=" + id + ")");
    }
}