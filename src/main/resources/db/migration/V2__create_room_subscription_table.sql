CREATE TABLE room_subscription
(
    user_id       BIGINT                       NOT NULL,
    room_id       BIGINT                       NOT NULL,
    status        ENUM ('APPROVED', 'REVOKED') NOT NULL,
    updated_at    TIMESTAMP                    NOT NULL,
    alarm_enable  BOOLEAN                      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (user_id, room_id),
    CONSTRAINT fk_room_subscription_user
        FOREIGN KEY (user_id) REFERENCES notification_user (user_id)
);
