-- ERD 리뷰 반영: PK 컬럼명 통일({테이블명}_id), bot_type/channel을 ENUM에서 VARCHAR로
-- (봇 타입이 늘어나도 DB 마이그레이션 없이 Java enum 상수만 추가하면 되도록).

ALTER TABLE telegram_subscription
    CHANGE COLUMN id telegram_sub_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN linked_at created_at TIMESTAMP NOT NULL,
    MODIFY COLUMN bot_type VARCHAR(20) NOT NULL;

ALTER TABLE alert_history
    CHANGE COLUMN id alert_history_id BIGINT NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN channel bot_type VARCHAR(20) NOT NULL,
    CHANGE COLUMN sent_at send_at TIMESTAMP NOT NULL,
    MODIFY COLUMN alert_type VARCHAR(50) NULL;
