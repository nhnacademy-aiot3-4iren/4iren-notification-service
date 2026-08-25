-- retention 배치가 매일 `DELETE FROM alert_history WHERE send_at < :cutoff LIMIT :batch`를
-- 청크 단위로 반복 실행한다. send_at에 인덱스가 없으면 매 청크마다 풀스캔이 돼서 청크 삭제의 이점이
-- 사라지므로, WHERE 조건 컬럼인 send_at에 인덱스를 추가한다.

CREATE INDEX idx_alert_history_send_at ON alert_history (send_at);
