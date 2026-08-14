package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.exception.OutsideWeatherSnapshotAlreadyExistsException;
import com.siren.notificationservice.core.repository.OutsideWeatherSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * OutsideWeatherSnapshot에 대한 순수 DB 접근 계층. Core API 호출이나 baseDateTime 파싱 같은
 * 오케스트레이션 로직은 이 클래스가 아니라 호출하는 쪽(FeedbackPersistenceService 등)의 책임이다.
 */
@Service
@RequiredArgsConstructor
public class OutsideWeatherSnapshotService {
    private final OutsideWeatherSnapshotRepository outsideWeatherSnapshotRepository;

    /**
     * weatherSnapshotId로 스냅샷 단건을 조회한다.
     */
    @Transactional(readOnly = true)
    public OutsideWeatherSnapshot getOutsideWeatherSnapshot(Long weatherSnapshotId) {
        Objects.requireNonNull(weatherSnapshotId, "weatherSnapshotId는 null일 수 없습니다.");
        return outsideWeatherSnapshotRepository.findById(weatherSnapshotId).orElse(null);
    }

    /**
     * weatherSnapshotId 목록으로 스냅샷 여러 건을 한 번에 조회한다. N+1 방지용 배치 조회.
     */
    @Transactional(readOnly = true)
    public List<OutsideWeatherSnapshot> getByIds(List<Long> weatherSnapshotIds) {
        Objects.requireNonNull(weatherSnapshotIds, "weatherSnapshotIds는 null일 수 없습니다.");
        return outsideWeatherSnapshotRepository.findAllById(weatherSnapshotIds);
    }

    /**
     * 같은 지역(nx, ny)·같은 시간대 스냅샷이 있으면 그 id를, 없으면 null을 반환한다.
     */
    @Transactional(readOnly = true)
    public OutsideWeatherSnapshot findSnapshotId(Integer nx, Integer ny, LocalDateTime windowStart) {
        Objects.requireNonNull(nx, "nx는 null일 수 없습니다.");
        Objects.requireNonNull(ny, "ny는 null일 수 없습니다.");
        Objects.requireNonNull(windowStart, "windowStart는 null일 수 없습니다.");
        return outsideWeatherSnapshotRepository.findByNxAndNyAndWindowStart(nx, ny, windowStart)
                .orElse(null);
    }

    /**
     * 새 외부 날씨 스냅샷을 만든다. 같은 (nx, ny, windowStart)가 이미 있으면 예외를 던진다.
     */
    @Transactional
    public OutsideWeatherSnapshot createOutsideWeatherSnapshot(Integer nx, Integer ny, LocalDateTime windowStart,
                                                                BigDecimal outsideTemperature, BigDecimal outsideHumidity) {
        Objects.requireNonNull(nx, "nx는 null일 수 없습니다.");
        Objects.requireNonNull(ny, "ny는 null일 수 없습니다.");
        Objects.requireNonNull(windowStart, "windowStart는 null일 수 없습니다.");
        if (findSnapshotId(nx, ny, windowStart) != null) {
            throw new OutsideWeatherSnapshotAlreadyExistsException(nx, ny, windowStart);
        }
        return outsideWeatherSnapshotRepository.save(
                OutsideWeatherSnapshot.builder()
                        .nx(nx)
                        .ny(ny)
                        .windowStart(windowStart)
                        .outsideTemperature(outsideTemperature)
                        .outsideHumidity(outsideHumidity)
                        .build()
        );
    }

    /**
     * weatherSnapshotId로 프록시 참조만 만든다 (DB 조회 없음). {findSnapshotId}로 이미
     * 존재를 확인한 id를 가지고, FeedbackLog 같은 다른 엔티티에 FK로 엮기만 할 때 쓴다.
     */
    public OutsideWeatherSnapshot getReferenceById(Long weatherSnapshotId) {
        Objects.requireNonNull(weatherSnapshotId, "weatherSnapshotId는 null일 수 없습니다.");
        return outsideWeatherSnapshotRepository.getReferenceById(weatherSnapshotId);
    }
}
