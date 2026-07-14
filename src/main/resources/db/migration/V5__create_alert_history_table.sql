CREATE TABLE alert_history
(
    id          BIGINT                         NOT NULL AUTO_INCREMENT,
    room_id     BIGINT                         NOT NULL,
    channel     ENUM ('ADMIN_BOT', 'USER_BOT') NOT NULL,
    alert_type  VARCHAR(50)                    NOT NULL,
    message     TEXT                           NOT NULL,
    sent_at     TIMESTAMP                      NOT NULL,
    user_id     BIGINT                         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_history_user
        FOREIGN KEY (user_id) REFERENCES notification_user (user_id)
);
