package com.siren.notificationservice.core.entity;

/**
 * alert_history에 기록되는 알림 종류. VARCHAR 컬럼에 이름으로 저장된다({@code @Enumerated(STRING)})
 * — 새 알림 종류가 추가돼도 DB 마이그레이션 없이 상수만 추가하면 된다.
 */
public enum AlertType {
    VENTILATION_RECOMMEND,
    SENSOR_ANOMALY
}
