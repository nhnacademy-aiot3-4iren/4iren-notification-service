-- outside_condition(강수형태)은 AI Learning이 실제 모델 학습에 쓰는 값이 아니라고 확인됨
-- (온도/습도만 필요) — 확인 안 된 데이터를 미리 저장해두지 않는다는 원칙에 따라 제거.
-- 나중에 필요해지면 nullable 컬럼 추가로 다시 대응.

ALTER TABLE outside_weather_snapshot
    DROP COLUMN outside_condition;
