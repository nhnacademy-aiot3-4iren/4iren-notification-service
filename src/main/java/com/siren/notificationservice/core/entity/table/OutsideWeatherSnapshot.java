package com.siren.notificationservice.core.entity.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * 외부(제3자) 날씨 API 실측값 한 시간 단위 스냅샷. room_environment_snapshot과 달리
 * room_id가 없다 — 이 배포(인스턴스)가 서비스하는 위치는 하나뿐이라 강의실별로 날씨가
 * 다르지 않기 때문. 여러 강의실의 피드백이 같은 시간대 값을 공유해서 재사용한다.
 *
 * windowStart는 우리가 임의로 반내림한 시각이 아니라, Core API가 날씨 API 응답에서
 * "이 값이 실제로 몇 시 데이터인지" 알려준 값(baseDateTime)을 그대로 쓴다 — 날씨 API가
 * 매시 15분에야 그 시간 데이터를 발행하는 지연이 있어서, 우리가 조회 시각을 그냥
 * 반내림하면 서로 다른 시간대 데이터가 같은 버킷으로 잘못 묶일 수 있기 때문이다.
 *
 * 강수형태(precipitationType) 컬럼은 두지 않는다 — AI Learning이 모델 학습에 실제로
 * 쓰는 건 온도/습도뿐이라고 확인됐고, 확인 안 된 값을 미리 저장해두는 건 이 프로젝트가
 * 계속 지켜온 "필요하다고 확인된 데이터만 저장" 원칙에 어긋난다. 나중에 필요해지면
 * nullable 컬럼 추가로 대응 가능.
 */
@Entity
@Table(name = "outside_weather_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_outside_weather_snapshot_window", columnNames = {"window_start"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OutsideWeatherSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weatherSnapshotId;

    @Column(name = "window_start", nullable = false)
    private ZonedDateTime windowStart; // Core가 알려준 "실제 데이터 시각" (우리가 계산한 반내림 시각 아님)

    @Column(name = "outside_temperature", precision = 4, scale = 1)
    private BigDecimal outsideTemperature; // nullable - 날씨 API 실패해도 피드백 자체는 저장돼야 하므로

    @Column(name = "outside_humidity", precision = 4, scale = 1)
    private BigDecimal outsideHumidity;
}
