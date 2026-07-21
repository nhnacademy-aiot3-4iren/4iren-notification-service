package com.siren.notificationservice.core.exception;

public class CoreApiUnavailableException extends NotificationServiceException {

    public CoreApiUnavailableException(Long userId) {
        super(ErrorCode.CORE_API_UNAVAILABLE, "Core API 응답 실패 (userId=" + userId + ")");
    }
}