ALTER TABLE feedback_log
    MODIFY COLUMN avg_temperature DOUBLE NULL,
    MODIFY COLUMN avg_humidity DOUBLE NULL,
    ADD COLUMN is_delayed BOOLEAN NOT NULL DEFAULT FALSE AFTER created_at,
    ADD COLUMN experienced_at TIMESTAMP NULL AFTER is_delayed;
