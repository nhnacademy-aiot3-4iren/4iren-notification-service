package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.exception.OutsideWeatherSnapshotAlreadyExistsException;
import com.siren.notificationservice.core.repository.OutsideWeatherSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutsideWeatherSnapshotServiceTest {

    private final OutsideWeatherSnapshotRepository outsideWeatherSnapshotRepository = mock(OutsideWeatherSnapshotRepository.class);
    private final OutsideWeatherSnapshotService outsideWeatherSnapshotService =
            new OutsideWeatherSnapshotService(outsideWeatherSnapshotRepository);

    private OutsideWeatherSnapshot snapshot(Long id, Integer nx, Integer ny, ZonedDateTime windowStart) {
        return OutsideWeatherSnapshot.builder()
                .weatherSnapshotId(id)
                .nx(nx)
                .ny(ny)
                .windowStart(windowStart)
                .outsideTemperature(BigDecimal.valueOf(28.4))
                .outsideHumidity(BigDecimal.valueOf(63.0))
                .build();
    }

    @Test
    void getOutsideWeatherSnapshotReturnsSnapshot() {
        OutsideWeatherSnapshot snapshot = snapshot(1L, 60, 127, ZonedDateTime.now());
        when(outsideWeatherSnapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));

        OutsideWeatherSnapshot result = outsideWeatherSnapshotService.getOutsideWeatherSnapshot(1L);

        assertThat(result).isEqualTo(snapshot);
    }

    @Test
    void getOutsideWeatherSnapshotReturnsNullWhenNotFound() {
        when(outsideWeatherSnapshotRepository.findById(1L)).thenReturn(Optional.empty());

        OutsideWeatherSnapshot result = outsideWeatherSnapshotService.getOutsideWeatherSnapshot(1L);

        assertThat(result).isNull();
    }

    @Test
    void getOutsideWeatherSnapshotThrowsWhenIdIsNull() {
        assertThatThrownBy(() -> outsideWeatherSnapshotService.getOutsideWeatherSnapshot(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getByIdsReturnsSnapshots() {
        OutsideWeatherSnapshot snapshot = snapshot(1L, 60, 127, ZonedDateTime.now());
        when(outsideWeatherSnapshotRepository.findAllById(List.of(1L))).thenReturn(List.of(snapshot));

        List<OutsideWeatherSnapshot> result = outsideWeatherSnapshotService.getByIds(List.of(1L));

        assertThat(result).containsExactly(snapshot);
    }

    @Test
    void getByIdsThrowsWhenIdsIsNull() {
        assertThatThrownBy(() -> outsideWeatherSnapshotService.getByIds(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void findSnapshotIdReturnsSnapshotWhenExists() {
        ZonedDateTime windowStart = ZonedDateTime.now();
        OutsideWeatherSnapshot snapshot = snapshot(1L, 60, 127, windowStart);
        when(outsideWeatherSnapshotRepository.findByNxAndNyAndWindowStart(60, 127, windowStart))
                .thenReturn(Optional.of(snapshot));

        OutsideWeatherSnapshot result = outsideWeatherSnapshotService.findSnapshotId(60, 127, windowStart);

        assertThat(result).isEqualTo(snapshot);
    }

    @Test
    void findSnapshotIdReturnsNullWhenNotExists() {
        ZonedDateTime windowStart = ZonedDateTime.now();
        when(outsideWeatherSnapshotRepository.findByNxAndNyAndWindowStart(60, 127, windowStart))
                .thenReturn(Optional.empty());

        OutsideWeatherSnapshot result = outsideWeatherSnapshotService.findSnapshotId(60, 127, windowStart);

        assertThat(result).isNull();
    }

    @Test
    void createOutsideWeatherSnapshotSavesNewSnapshot() {
        ZonedDateTime windowStart = ZonedDateTime.now();
        when(outsideWeatherSnapshotRepository.findByNxAndNyAndWindowStart(60, 127, windowStart))
                .thenReturn(Optional.empty());
        OutsideWeatherSnapshot saved = snapshot(1L, 60, 127, windowStart);
        when(outsideWeatherSnapshotRepository.save(any())).thenReturn(saved);

        OutsideWeatherSnapshot result = outsideWeatherSnapshotService.createOutsideWeatherSnapshot(
                60, 127, windowStart, BigDecimal.valueOf(28.4), BigDecimal.valueOf(63.0));

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createOutsideWeatherSnapshotThrowsWhenAlreadyExists() {
        ZonedDateTime windowStart = ZonedDateTime.now();
        OutsideWeatherSnapshot existing = snapshot(1L, 60, 127, windowStart);
        when(outsideWeatherSnapshotRepository.findByNxAndNyAndWindowStart(60, 127, windowStart))
                .thenReturn(Optional.of(existing));
        BigDecimal outsideTemperature = BigDecimal.valueOf(28.4);
        BigDecimal outsideHumidity = BigDecimal.valueOf(63.0);

        assertThatThrownBy(() -> outsideWeatherSnapshotService.createOutsideWeatherSnapshot(
                60, 127, windowStart, outsideTemperature, outsideHumidity))
                .isInstanceOf(OutsideWeatherSnapshotAlreadyExistsException.class);
    }

    @Test
    void getReferenceByIdReturnsProxy() {
        OutsideWeatherSnapshot proxy = snapshot(1L, 60, 127, ZonedDateTime.now());
        when(outsideWeatherSnapshotRepository.getReferenceById(1L)).thenReturn(proxy);

        OutsideWeatherSnapshot result = outsideWeatherSnapshotService.getReferenceById(1L);

        assertThat(result).isEqualTo(proxy);
    }

    @Test
    void getReferenceByIdThrowsWhenIdIsNull() {
        assertThatThrownBy(() -> outsideWeatherSnapshotService.getReferenceById(null))
                .isInstanceOf(NullPointerException.class);
    }
}
