package com.siren.notificationservice.core.entity;

/**
 * feedback_score의 key-value 축 종류. 사용자가 자연어로 언급한 "체감" 대상을 나타낸다
 * (실제 센서 측정값이 아니라 -2~2 주관적 점수). VARCHAR 컬럼에 이름으로 저장되므로
 * 새 축이 추가돼도 DB 마이그레이션 없이 상수만 추가하면 된다.
 */
public enum SensorType {
    TEMPERATURE,
    HUMIDITY,
    AIR_QUALITY
}
