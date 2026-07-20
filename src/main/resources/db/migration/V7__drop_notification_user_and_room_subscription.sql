-- ERD 리뷰 반영: notification_user/room_subscription은 Account API/Core API 데이터와 중복이라 제거.
-- 다른 테이블은 이제 순수 user_id 값만 들고 있고(로컬 FK 없음), 실시간 조회가 필요해지면 Feign으로 대체.

ALTER TABLE telegram_subscription DROP FOREIGN KEY fk_telegram_subscription_user;
ALTER TABLE feedback_log DROP FOREIGN KEY fk_feedback_log_user;
ALTER TABLE alert_history DROP FOREIGN KEY fk_alert_history_user;

DROP TABLE room_subscription;
DROP TABLE notification_user;
