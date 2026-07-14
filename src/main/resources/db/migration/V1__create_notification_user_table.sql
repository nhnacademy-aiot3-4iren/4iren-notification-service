CREATE TABLE notification_user
(
    user_id    BIGINT       NOT NULL,
    user_name  VARCHAR(50)  NOT NULL,
    is_active  BOOLEAN      NOT NULL,
    synced_at  TIMESTAMP    NOT NULL,
    PRIMARY KEY (user_id)
);
