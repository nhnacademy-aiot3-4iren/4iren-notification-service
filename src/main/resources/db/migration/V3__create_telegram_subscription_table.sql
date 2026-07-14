CREATE TABLE telegram_subscription
(
    id         BIGINT                          NOT NULL AUTO_INCREMENT,
    bot_type   ENUM ('ADMIN_BOT', 'USER_BOT')  NOT NULL,
    chat_id    VARCHAR(50)                     NOT NULL,
    is_active  BOOLEAN                         NOT NULL DEFAULT TRUE,
    linked_at  TIMESTAMP                       NOT NULL,
    user_id    BIGINT                          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_telegram_subscription_user_bot_type UNIQUE (user_id, bot_type),
    CONSTRAINT fk_telegram_subscription_user
        FOREIGN KEY (user_id) REFERENCES notification_user (user_id)
);
