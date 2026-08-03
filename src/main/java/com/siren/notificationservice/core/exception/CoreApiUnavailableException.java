package com.siren.notificationservice.core.exception;

public class CoreApiUnavailableException extends RuntimeException {

    public CoreApiUnavailableException(Long id, String message) {
        super("Core API 응답 실패 (" + message + "=" + id + ")");
    }
}