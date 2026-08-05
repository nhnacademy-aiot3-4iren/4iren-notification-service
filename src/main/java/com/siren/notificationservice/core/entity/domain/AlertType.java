package com.siren.notificationservice.core.entity.domain;

import lombok.Getter;

/**
 * alert_history에 기록되는 알림 종류. VARCHAR 컬럼에 이름으로 저장된다({@code @Enumerated(STRING)})
 * — 새 알림 종류가 추가돼도 DB 마이그레이션 없이 상수만 추가하면 된다.
 */
@Getter
public enum AlertType {
    VENTILATION_RECOMMEND("제안"), // 실내 환기를 권장하는 가벼운 상태
    SENSOR_ANOMALY("센서 장비 이상"), //센서 자체를 못 믿겠다
    COMFORT_LIMIT_EXCEEDED("위험 임계치 초과"); //인간의 생체적/물리적 불쾌감이 극에 달해 즉각적인 조치가 필요한 상태

    private final String description;

    AlertType(String description) {
        this.description = description;
    }

    /**
     * 긴급도 수준을 나타냄. SENSOR_ANOMALY, COMFORT_LIMIT_EXCEEDED는 긴급.
     */
    public boolean isUrgent() {
        return this != VENTILATION_RECOMMEND;
    }
}
