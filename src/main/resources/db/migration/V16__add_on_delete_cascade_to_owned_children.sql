-- feedback_score / room_environment_reading은 각각 부모(feedback_log / room_environment_snapshot)에
-- 완전히 종속된 자식(부모 없으면 의미가 없는 소유 관계)이라, 부모 삭제 시 함께 지워지는 게 맞다.
-- retention 배치가 부모를 벌크 native DELETE로 지울 때 자식을 별도로 먼저 지우는 단계(및 FK 순서 관리)를
-- 없애려고 FK에 ON DELETE CASCADE를 부여한다. (JPA @OneToMany cascade는 벌크 DELETE에 안 먹으므로 DB 레벨로 처리)

ALTER TABLE feedback_score
    DROP FOREIGN KEY fk_feedback_score_feedback_log,
    ADD CONSTRAINT fk_feedback_score_feedback_log
        FOREIGN KEY (feedback_log_id) REFERENCES feedback_log (feedback_log_id) ON DELETE CASCADE;

ALTER TABLE room_environment_reading
    DROP FOREIGN KEY fk_room_environment_reading_snapshot,
    ADD CONSTRAINT fk_room_environment_reading_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES room_environment_snapshot (snapshot_id) ON DELETE CASCADE;
