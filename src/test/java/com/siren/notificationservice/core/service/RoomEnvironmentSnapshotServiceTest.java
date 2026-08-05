package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.exception.RoomEnvironmentSnapshotAlreadyExistsException;
import com.siren.notificationservice.core.repository.RoomEnvironmentSnapshotRepository;
import com.siren.notificationservice.core.service.basic_service.RoomEnvironmentReadingService;
import com.siren.notificationservice.core.service.basic_service.RoomEnvironmentSnapshotService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomEnvironmentSnapshotServiceTest {

    private final RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository = mock(RoomEnvironmentSnapshotRepository.class);
    private final RoomEnvironmentReadingService roomEnvironmentReadingService = mock(RoomEnvironmentReadingService.class);
    private final RoomEnvironmentSnapshotService roomEnvironmentSnapshotService =
            new RoomEnvironmentSnapshotService(roomEnvironmentSnapshotRepository, roomEnvironmentReadingService);

    private RoomEnvironmentSnapshot snapshot(Long id, Long roomId, ZonedDateTime windowStart) {
        return RoomEnvironmentSnapshot.builder()
                .snapshotId(id)
                .roomId(roomId)
                .windowStart(windowStart)
                .build();
    }

    @Test
    void getRoomEnvironmentSnapshotReturnsSnapshot() {
        RoomEnvironmentSnapshot snapshot = snapshot(1L, 7L, ZonedDateTime.now());
        when(roomEnvironmentSnapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));

        RoomEnvironmentSnapshot result = roomEnvironmentSnapshotService.getRoomEnvironmentSnapshot(1L);

        assertThat(result).isEqualTo(snapshot);
    }

    @Test
    void getRoomEnvironmentSnapshotThrowsWhenIdIsNull() {
        assertThatThrownBy(() -> roomEnvironmentSnapshotService.getRoomEnvironmentSnapshot(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getRoomEnvironmentSnapshotByRoomIdReturnsSnapshots() {
        RoomEnvironmentSnapshot snapshot = snapshot(1L, 7L, ZonedDateTime.now());
        when(roomEnvironmentSnapshotRepository.findByRoomId(7L)).thenReturn(List.of(snapshot));

        List<RoomEnvironmentSnapshot> result = roomEnvironmentSnapshotService.getRoomEnvironmentSnapshotByRoomId(7L);

        assertThat(result).containsExactly(snapshot);
    }

    @Test
    void getRoomEnvironmentSnapshotByRoomIdThrowsWhenRoomIdIsNull() {
        assertThatThrownBy(() -> roomEnvironmentSnapshotService.getRoomEnvironmentSnapshotByRoomId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void findSnapshotIdReturnsSnapshotWhenCovered() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        RoomEnvironmentSnapshot snapshot = snapshot(1L, 7L, referenceAt);
        when(roomEnvironmentSnapshotRepository.findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(
                7L, referenceAt, referenceAt.plusMinutes(15))).thenReturn(Optional.of(snapshot));

        RoomEnvironmentSnapshot result = roomEnvironmentSnapshotService.findSnapshotId(7L, referenceAt);

        assertThat(result).isEqualTo(snapshot);
    }

    @Test
    void findSnapshotIdReturnsNullWhenNoneCovers() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        when(roomEnvironmentSnapshotRepository.findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(
                any(), any(), any())).thenReturn(Optional.empty());

        RoomEnvironmentSnapshot result = roomEnvironmentSnapshotService.findSnapshotId(7L, referenceAt);

        assertThat(result).isNull();
    }

    @Test
    void getReferenceByIdReturnsProxy() {
        RoomEnvironmentSnapshot proxy = snapshot(1L, 7L, ZonedDateTime.now());
        when(roomEnvironmentSnapshotRepository.getReferenceById(1L)).thenReturn(proxy);

        RoomEnvironmentSnapshot result = roomEnvironmentSnapshotService.getReferenceById(1L);

        assertThat(result).isEqualTo(proxy);
    }

    @Test
    void createWithReadingsSavesSnapshotAndSkipsUnknownMetrics() {
        Long roomId = 7L;
        ZonedDateTime referenceAt = ZonedDateTime.now();
        when(roomEnvironmentSnapshotRepository.findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(
                any(), any(), any())).thenReturn(Optional.empty());
        RoomEnvironmentSnapshot savedSnapshot = snapshot(1L, roomId, referenceAt);
        when(roomEnvironmentSnapshotRepository.save(any())).thenReturn(savedSnapshot);

        RoomEnvironmentReadingResponse readings = new RoomEnvironmentReadingResponse(roomId, Instant.now(), List.of(
                new RoomEnvironmentReadingResponse.MetricAverage("temperature", 24.5),
                new RoomEnvironmentReadingResponse.MetricAverage("illuminance", 300.0) // 우리가 안 쓰는 센서, 걸러져야 함
        ));

        RoomEnvironmentSnapshot result = roomEnvironmentSnapshotService.createWithReadings(roomId, referenceAt, readings);

        assertThat(result).isEqualTo(savedSnapshot);
        verify(roomEnvironmentReadingService).saveAll(argThat(list -> list.size() == 1));
    }

    @Test
    void createWithReadingsThrowsWhenReadingsIsNull() {
        ZonedDateTime referenceAt = ZonedDateTime.now();

        assertThatThrownBy(() -> roomEnvironmentSnapshotService.createWithReadings(7L, referenceAt, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createWithReadingsThrowsWhenSnapshotAlreadyExists() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        RoomEnvironmentSnapshot existing = snapshot(1L, 7L, referenceAt);
        when(roomEnvironmentSnapshotRepository.findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(
                any(), any(), any())).thenReturn(Optional.of(existing));
        RoomEnvironmentReadingResponse readings = new RoomEnvironmentReadingResponse(7L, Instant.now(), List.of());

        assertThatThrownBy(() -> roomEnvironmentSnapshotService.createWithReadings(7L, referenceAt, readings))
                .isInstanceOf(RoomEnvironmentSnapshotAlreadyExistsException.class);
    }
}
