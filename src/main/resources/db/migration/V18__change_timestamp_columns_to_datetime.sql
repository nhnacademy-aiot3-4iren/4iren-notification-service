-- 엔티티 시각 타입을 ZonedDateTime → LocalDateTime으로 통일하면서
-- 컬럼 타입 TIMESTAMP(UTC 정규화 저장) → DATETIME(넣은 KST wall-clock 그대로 저장)으로 변경.
-- KST 단일 리전 서비스라 tz 변환이 불필요하고, TIMESTAMP UTC 저장으로 인한 표시 혼란/2038 한계를 제거한다.
-- DATETIME(6): Hibernate의 LocalDateTime 기본 매핑과 일치(마이크로초 정밀도).

ALTER TABLE telegram_subscription
    MODIFY COLUMN created_at      DATETIME(6) NOT NULL,
    MODIFY COLUMN role_updated_at DATETIME(6) NULL;

ALTER TABLE feedback_log
    MODIFY COLUMN created_at     DATETIME(6) NOT NULL,
    MODIFY COLUMN experienced_at DATETIME(6) NULL;

ALTER TABLE alert_history
    MODIFY COLUMN send_at DATETIME(6) NOT NULL;

ALTER TABLE outside_weather_snapshot
    MODIFY COLUMN window_start DATETIME(6) NOT NULL;

ALTER TABLE room_environment_snapshot
    MODIFY COLUMN window_start DATETIME(6) NOT NULL;
