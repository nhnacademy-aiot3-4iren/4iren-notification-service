-- ERD 리뷰 반영: feedback_log에 섞여있던 관심사를 분리.
-- 1) 축별 체감 점수(temp/humidity/air_quality) -> feedback_score(key-value, 확장 가능)
-- 2) 실내 환경 실측값(15분 평균) -> room_environment_snapshot + room_environment_reading(key-value, 확장 가능)
--    같은 강의실의 같은 15분 구간을 여러 피드백이 공유하므로 중복 저장 방지.

CREATE TABLE room_environment_snapshot
(
    snapshot_id  BIGINT    NOT NULL AUTO_INCREMENT,
    room_id      BIGINT    NOT NULL,
    window_start TIMESTAMP NOT NULL,
    PRIMARY KEY (snapshot_id),
    CONSTRAINT uq_room_environment_snapshot_room_window UNIQUE (room_id, window_start)
);

CREATE TABLE room_environment_reading
(
    snapshot_id BIGINT         NOT NULL,
    metric_type VARCHAR(30)    NOT NULL,
    value       DECIMAL(10, 3) NOT NULL,
    PRIMARY KEY (snapshot_id, metric_type),
    CONSTRAINT fk_room_environment_reading_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES room_environment_snapshot (snapshot_id)
);

CREATE TABLE feedback_score
(
    feedback_log_id BIGINT      NOT NULL,
    sensor_type     VARCHAR(30) NOT NULL,
    score           INTEGER     NOT NULL,
    PRIMARY KEY (feedback_log_id, sensor_type),
    CONSTRAINT fk_feedback_score_feedback_log
        FOREIGN KEY (feedback_log_id) REFERENCES feedback_log (feedback_log_id)
);

ALTER TABLE feedback_log
    DROP COLUMN avg_temperature,
    DROP COLUMN avg_humidity,
    DROP COLUMN avg_co2,
    DROP COLUMN temp_score,
    DROP COLUMN humidity_score,
    DROP COLUMN air_quality_score,
    ADD COLUMN snapshot_id BIGINT NULL AFTER room_id,
    MODIFY COLUMN raw_text TEXT NOT NULL,
    MODIFY COLUMN outside_temperature DECIMAL(4, 1) NOT NULL,
    MODIFY COLUMN outside_humidity DECIMAL(4, 1) NULL,
    ADD CONSTRAINT fk_feedback_log_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES room_environment_snapshot (snapshot_id);
