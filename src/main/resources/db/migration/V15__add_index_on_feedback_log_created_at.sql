-- feedback_log retention 배치가 `SELECT feedback_log_id ... WHERE created_at < :cutoff LIMIT :batch`로
-- 오래된(90일 초과) 로그 id를 청크 단위로 뽑아 자식(feedback_score)→부모(feedback_log) 순으로 삭제한다.
-- created_at에 인덱스가 없으면 이 범위 조회가 매 청크마다 풀스캔이 되므로 인덱스를 추가한다.

CREATE INDEX idx_feedback_log_created_at ON feedback_log (created_at);
