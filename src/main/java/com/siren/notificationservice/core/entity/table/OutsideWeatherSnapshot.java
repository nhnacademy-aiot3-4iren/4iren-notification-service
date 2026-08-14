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
import java.time.LocalDateTime;

/**
 * 외부(제3자) 날씨 API 실측값 한 시간 단위 스냅샷. room_environment_snapshot과 달리
 * room_id가 없다 — 강의실이 다르다고 날씨가 다른 게 아니라, 강의실이 위치한 지역
 * (기상청 격자 좌표 nx/ny)이 같으면 날씨도 같기 때문. 같은 지역의 여러 강의실/피드백이
 * 같은 시간대 값을 공유해서 재사용한다.
 *
 * nx/ny를 window_start와 함께 유니크 키에 두는 이유: 지금은 이 배포(인스턴스)가 서비스하는
 * 위치가 하나뿐이라 nx/ny가 항상 같은 값이지만, 나중에 여러 지역을 한 배포에서 같이
 * 서비스하게 되면 같은 시간대라도 지역마다 날씨가 다를 수 있다 — window_start만으로
 * 매칭하면 서로 다른 지역의 날씨가 하나로 잘못 묶인다.
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
                @UniqueConstraint(name = "uq_outside_weather_snapshot_region_window", columnNames = {"nx", "ny", "window_start"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OutsideWeatherSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weatherSnapshotId;

    @Column(name = "nx", nullable = false)
    private Integer nx; // 기상청 격자 좌표 X

    @Column(name = "ny", nullable = false)
    private Integer ny; // 기상청 격자 좌표 Y

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart; // Core가 알려준 "실제 데이터 시각" (우리가 계산한 반내림 시각 아님)

    @Column(name = "outside_temperature", precision = 4, scale = 1)
    private BigDecimal outsideTemperature; // nullable - 날씨 API 실패해도 피드백 자체는 저장돼야 하므로

    @Column(name = "outside_humidity", precision = 4, scale = 1)
    private BigDecimal outsideHumidity;
}
