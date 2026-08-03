-- 다중 지역 서비스를 대비해 외부 날씨 스냅샷의 유니크 키에 기상청 격자 좌표(nx, ny)를 추가한다.
-- 지금은 배포 하나당 위치가 하나뿐이라 nx/ny가 항상 같은 값이지만, 나중에 여러 위치를 한
-- 배포에서 같이 서비스하게 되면 같은 window_start라도 지역마다 다른 날씨를 구분해야 한다.

ALTER TABLE outside_weather_snapshot
    DROP INDEX uq_outside_weather_snapshot_window,
    ADD COLUMN nx INT NOT NULL AFTER weather_snapshot_id,
    ADD COLUMN ny INT NOT NULL AFTER nx,
    ADD CONSTRAINT uq_outside_weather_snapshot_region_window UNIQUE (nx, ny, window_start);
