CREATE TABLE feedback_log
(
    feedback_log_id       BIGINT       NOT NULL AUTO_INCREMENT,
    room_id               BIGINT       NOT NULL,
    avg_temperature       DOUBLE       NOT NULL,
    avg_humidity          DOUBLE       NOT NULL,
    avg_co2               DOUBLE       NULL,
    temp_score            INTEGER      NULL,
    humidity_score        INTEGER      NULL,
    air_quality_score     INTEGER      NULL,
    raw_text              TEXT         NULL,
    created_at            TIMESTAMP    NOT NULL,
    outside_temperature   DOUBLE       NOT NULL,
    outside_humidity      DOUBLE       NULL,
    outside_condition     VARCHAR(10)  NULL,
    user_id               BIGINT       NOT NULL,
    PRIMARY KEY (feedback_log_id),
    CONSTRAINT fk_feedback_log_user
        FOREIGN KEY (user_id) REFERENCES notification_user (user_id)
);
