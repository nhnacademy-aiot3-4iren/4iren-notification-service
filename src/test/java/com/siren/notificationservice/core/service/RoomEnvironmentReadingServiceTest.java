package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.repository.RoomEnvironmentReadingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomEnvironmentReadingServiceTest {

    private final RoomEnvironmentReadingRepository roomEnvironmentReadingRepository = mock(RoomEnvironmentReadingRepository.class);
    private final RoomEnvironmentReadingService roomEnvironmentReadingService =
            new RoomEnvironmentReadingService(roomEnvironmentReadingRepository);

    private RoomEnvironmentReading reading(Long snapshotId) {
        return RoomEnvironmentReading.builder()
                .id(RoomEnvironmentReadingId.builder().snapshotId(snapshotId).metricType(EnvironmentMetricType.TEMPERATURE).build())
                .value(BigDecimal.valueOf(24.5))
                .build();
    }

    @Test
    void getReadingsBySnapshotIdReturnsReadings() {
        RoomEnvironmentReading reading = reading(1L);
        when(roomEnvironmentReadingRepository.findById_SnapshotId(1L)).thenReturn(List.of(reading));

        List<RoomEnvironmentReading> result = roomEnvironmentReadingService.getReadingsBySnapshotId(1L);

        assertThat(result).containsExactly(reading);
    }

    @Test
    void getReadingsBySnapshotIdThrowsWhenIdIsNull() {
        assertThatThrownBy(() -> roomEnvironmentReadingService.getReadingsBySnapshotId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getReadingsBySnapshotIdsReturnsReadings() {
        RoomEnvironmentReading reading = reading(1L);
        when(roomEnvironmentReadingRepository.findById_SnapshotIdIn(List.of(1L, 2L))).thenReturn(List.of(reading));

        List<RoomEnvironmentReading> result = roomEnvironmentReadingService.getReadingsBySnapshotIds(List.of(1L, 2L));

        assertThat(result).containsExactly(reading);
    }

    @Test
    void getReadingsBySnapshotIdsThrowsWhenIdsIsNull() {
        assertThatThrownBy(() -> roomEnvironmentReadingService.getReadingsBySnapshotIds(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void saveAllSavesReadings() {
        List<RoomEnvironmentReading> readings = List.of(reading(1L));

        roomEnvironmentReadingService.saveAll(readings);

        verify(roomEnvironmentReadingRepository).saveAll(readings);
    }

    @Test
    void saveAllThrowsWhenReadingsIsNull() {
        assertThatThrownBy(() -> roomEnvironmentReadingService.saveAll(null))
                .isInstanceOf(NullPointerException.class);
    }
}
