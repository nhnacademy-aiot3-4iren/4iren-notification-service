package com.siren.notificationservice.core.entity.domain;

/**
 * room_environment_reading의 key-value 축 종류. Core API 센서가 실제로 측정한 값을 나타낸다
 * (주관적 점수가 아니라 DECIMAL 실측값). VARCHAR 컬럼에 이름으로 저장되므로 센서가
 * 추가돼도 DB 마이그레이션 없이 상수만 추가하면 된다.
 */
public enum EnvironmentMetricType {
    TEMPERATURE,
    HUMIDITY,
    CO2
}
