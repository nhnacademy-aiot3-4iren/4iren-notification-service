-- feedback_log에 박혀있던 외부(제3자) 날씨 값을 별도 테이블로 분리.
-- room_environment_snapshot과 같은 이유(같은 시간대 값을 여러 피드백이 중복 저장하던 낭비)이지만,
-- 날씨는 강의실 단위가 아니라 이 배포(인스턴스)가 서비스하는 위치 전체에 하나뿐이라 room_id가 없다.
-- window_start는 우리가 계산한 반내림 시각이 아니라 Core API가 알려주는 "실제 날씨 데이터 시각"을
-- 그대로 쓴다 — 날씨 API가 매시 15분 지연 발행되는 특성 때문에 우리가 임의로 시각을 반내림하면
-- 서로 다른 시간대 데이터가 같은 버킷으로 잘못 묶일 수 있어서다.

CREATE TABLE outside_weather_snapshot
(
    weather_snapshot_id BIGINT      NOT NULL AUTO_INCREMENT,
    window_start        TIMESTAMP   NOT NULL,
    outside_temperature DECIMAL(4, 1) NULL,
    outside_humidity    DECIMAL(4, 1) NULL,
    outside_condition   VARCHAR(10) NULL,
    PRIMARY KEY (weather_snapshot_id),
    CONSTRAINT uq_outside_weather_snapshot_window UNIQUE (window_start)
);

ALTER TABLE feedback_log
    DROP COLUMN outside_temperature,
    DROP COLUMN outside_humidity,
    DROP COLUMN outside_condition,
    ADD COLUMN outside_weather_snapshot_id BIGINT NULL AFTER snapshot_id,
    ADD CONSTRAINT fk_feedback_log_outside_weather_snapshot
        FOREIGN KEY (outside_weather_snapshot_id) REFERENCES outside_weather_snapshot (weather_snapshot_id);
