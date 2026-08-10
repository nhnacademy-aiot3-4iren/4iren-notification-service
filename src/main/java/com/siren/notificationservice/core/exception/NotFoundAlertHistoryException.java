package com.siren.notificationservice.core.exception;

public class NotFoundAlertHistoryException extends RuntimeException {
    public NotFoundAlertHistoryException(Long alertHistoryId) {
        super("알림 히스토리 상세내역을 찾을 수 없습니다 alertHistoryId="+alertHistoryId);
    }
}
