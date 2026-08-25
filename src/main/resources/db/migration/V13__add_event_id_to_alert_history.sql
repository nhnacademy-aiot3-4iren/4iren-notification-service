-- Processing/RuleEngine이 dedupWindowSec 경과 후 같은 이벤트를 정상적으로 재발행할 수 있어서,
-- event_id 단독으로는 중복 판단 기준이 될 수 없다. alert_history는 알림 하나가 여러 수신자에게
-- 팬아웃되어 유저별로 한 row씩 쌓이고, 한 유저가 Admin/Member 봇 둘 다 연동돼 있으면 같은 이벤트로
-- 봇타입별 row가 각각 생길 수 있으므로, "이 유저의 이 봇으로 이 이벤트를 이미 보냈는가"를 판단하는
-- (event_id, user_id, bot_type) 복합 UNIQUE로 재처리(DLQ 재시도 등)로 인한 중복 발송만 막는다.

ALTER TABLE alert_history
    ADD COLUMN event_id VARCHAR(100) NOT NULL AFTER alert_type,
    ADD CONSTRAINT uq_alert_history_event_user_bot UNIQUE (event_id, user_id, bot_type);
