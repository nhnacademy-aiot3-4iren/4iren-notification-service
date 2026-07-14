package com.siren.notificationservice.core.exception;

public class NotificationUserNotFoundException extends NotificationServiceException {

    /**
     * 조회와 최소-정보 생성, 재조회까지 전부 실패해 유저를 확정할 수 없을 때 던진다.
     *
     * @param userId 확정하지 못한 유저 id
     */
    public NotificationUserNotFoundException(Long userId) {
        super(ErrorCode.NOTIFICATION_USER_NOT_FOUND, "userId=" + userId);
    }
}
