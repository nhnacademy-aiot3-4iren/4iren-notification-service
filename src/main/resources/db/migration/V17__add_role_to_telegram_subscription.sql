-- 유저 role(OWNER/ADMIN/NORMAL)을 telegram_subscription에 로컬 보관한다.
-- 텔레그램 QUESTION 흐름은 게이트웨이를 안 거쳐 X-USER-ROLE이 없으므로, notification이 userId 기준
-- role을 알고 있어야 Recommendation 호출 시 X-USER-ROLE을 채워 넘길 수 있다(웹은 게이트웨이가, 텔레그램은
-- notification이 role을 주입 → Recommendation 계약 균일).
--
-- role은 Account가 소유하는 값이라 매 요청 Feign 조회 대신, Account가 RabbitMQ로 (userId, role, 변경시각)
-- 이벤트를 발행하면 리스너가 해당 userId의 모든 row를 갱신한다(로컬 미러 + 이벤트 동기화).
-- [예외 기록] CLAUDE.md 원칙 2(유저 정보 로컬 미러링 금지)를 role에 한해 되돌리는 결정 —
-- role은 거의 안 바뀌는데 QUESTION은 잦아 미러가 효율적이고 Account 장애에 강하다. 근거는 아키텍처-Q&A 참고.
--
-- role_updated_at: stale 이벤트 가드용. 리스너는 이벤트 변경시각이 이 값보다 오래되면 무시한다
--                  (순서 뒤바뀐 이벤트가 최신 role을 덮어쓰지 않도록).
-- user_role은 NOT NULL — 텔레그램 연동은 항상 "가입(=role 존재) 후 연동"이라 생성 시점에 role이 반드시 있다
-- (딥링크 발급 시 게이트웨이의 X-USER-ROLE을 토큰에 실어 저장 → /start 연동 시 세팅). DEFAULT 'NORMAL'은
-- 기존 row 백필 + 혹시 미세팅 시의 최소권한 안전판. role_updated_at은 nullable(최초 연동 땐 이벤트가 없어 NULL,
-- stale 가드가 null 체크로 처리).
-- bot_type처럼 VARCHAR + Java enum(@Enumerated STRING)으로 두어, role 값이 늘어도 마이그레이션이 불필요.

ALTER TABLE telegram_subscription
    ADD COLUMN user_role       VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER user_id,
    ADD COLUMN role_updated_at TIMESTAMP   NULL     DEFAULT NULL     AFTER user_role;
