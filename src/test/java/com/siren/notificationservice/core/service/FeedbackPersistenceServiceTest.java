package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.RoomWeatherRegion;
import com.siren.notificationservice.core.dto.response.OutsideWeather;
import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.cache.RoomWeatherRegionCacheService;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackPersistenceServiceTest {

    private final RoomEnvironmentSnapshotService roomEnvironmentSnapshotService = mock(RoomEnvironmentSnapshotService.class);
    private final OutsideWeatherSnapshotService outsideWeatherSnapshotService = mock(OutsideWeatherSnapshotService.class);
    private final RoomWeatherRegionCacheService regionCacheService = mock(RoomWeatherRegionCacheService.class);
    private final FeedbackLogService feedbackLogService = mock(FeedbackLogService.class);
    private final CoreApiClient coreApiClient = mock(CoreApiClient.class);
    private final FeedbackPersistenceService feedbackPersistenceService = new FeedbackPersistenceService(
            roomEnvironmentSnapshotService, outsideWeatherSnapshotService, regionCacheService, feedbackLogService, coreApiClient);

    private FeedbackProcessingEvent event() {
        return new FeedbackProcessingEvent(1L, 7L, "더워요", List.of(), false, null, ZonedDateTime.now());
    }

    @Test
    void persistUsesExistingSnapshotsWithoutCallingCore() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        RoomEnvironmentSnapshot roomSnapshot = RoomEnvironmentSnapshot.builder()
                .snapshotId(10L).roomId(7L).windowStart(referenceAt).build();
        OutsideWeatherSnapshot weatherSnapshot = OutsideWeatherSnapshot.builder()
                .weatherSnapshotId(20L).nx(60).ny(127).windowStart(referenceAt).build();
        when(roomEnvironmentSnapshotService.findSnapshotId(7L, referenceAt)).thenReturn(roomSnapshot);
        when(regionCacheService.find(7L)).thenReturn(Optional.of(new RoomWeatherRegion(60, 127)));
        when(outsideWeatherSnapshotService.findSnapshotId(60, 127, referenceAt)).thenReturn(weatherSnapshot);

        feedbackPersistenceService.persist(event(), referenceAt);

        verify(coreApiClient, never()).getRoomSensorsReadings(any(), any());
        verify(coreApiClient, never()).getOutsideWeather(any(), any());
        verify(feedbackLogService).createFeedbackLogWithScores(
                eq(1L), eq(7L), eq(roomSnapshot), eq(weatherSnapshot), eq("더워요"),
                any(), eq(false), isNull(), eq(List.of()));
    }

    @Test
    void persistCreatesRoomSnapshotFromCoreWhenNoneCachedLocally() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        when(roomEnvironmentSnapshotService.findSnapshotId(7L, referenceAt)).thenReturn(null);
        RoomEnvironmentReadingResponse readingResponse = new RoomEnvironmentReadingResponse(7L, null, List.of());
        when(coreApiClient.getRoomSensorsReadings(7L, referenceAt.toLocalDateTime())).thenReturn(readingResponse);
        RoomEnvironmentSnapshot createdSnapshot = RoomEnvironmentSnapshot.builder()
                .snapshotId(11L).roomId(7L).windowStart(referenceAt).build();
        when(roomEnvironmentSnapshotService.createWithReadings(7L, referenceAt, readingResponse)).thenReturn(createdSnapshot);
        when(regionCacheService.find(7L)).thenReturn(Optional.empty());
        when(coreApiClient.getOutsideWeather(any(), any())).thenThrow(new CoreApiUnavailableException(7L, "roomId"));

        feedbackPersistenceService.persist(event(), referenceAt);

        verify(feedbackLogService).createFeedbackLogWithScores(
                eq(1L), eq(7L), eq(createdSnapshot), isNull(), eq("더워요"),
                any(), eq(false), isNull(), eq(List.of()));
    }

    @Test
    void persistKeepsGoingWithNullSnapshotWhenCoreFailsForRoomReadings() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        when(roomEnvironmentSnapshotService.findSnapshotId(7L, referenceAt)).thenReturn(null);
        when(coreApiClient.getRoomSensorsReadings(7L, referenceAt.toLocalDateTime()))
                .thenThrow(new CoreApiUnavailableException(7L, "roomId"));
        when(regionCacheService.find(7L)).thenReturn(Optional.empty());
        when(coreApiClient.getOutsideWeather(any(), any())).thenThrow(new CoreApiUnavailableException(7L, "roomId"));

        feedbackPersistenceService.persist(event(), referenceAt);

        verify(roomEnvironmentSnapshotService, never()).createWithReadings(any(), any(), any());
        verify(feedbackLogService).createFeedbackLogWithScores(
                eq(1L), eq(7L), isNull(), isNull(), eq("더워요"),
                any(), eq(false), isNull(), eq(List.of()));
    }

    @Test
    void persistCreatesWeatherSnapshotAndCachesRegionWhenNotCachedYet() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        when(roomEnvironmentSnapshotService.findSnapshotId(7L, referenceAt)).thenReturn(
                RoomEnvironmentSnapshot.builder().snapshotId(10L).roomId(7L).windowStart(referenceAt).build());
        when(regionCacheService.find(7L)).thenReturn(Optional.empty());
        OutsideWeather weather = new OutsideWeather("2026-07-24 13:00", "33.7℃", "69%", 60, 127);
        when(coreApiClient.getOutsideWeather(7L, referenceAt.toLocalDateTime())).thenReturn(weather);
        ZonedDateTime windowStart = LocalDateTime
                .parse("2026-07-24 13:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                .atZone(ZoneId.of("Asia/Seoul"));
        when(outsideWeatherSnapshotService.findSnapshotId(60, 127, windowStart)).thenReturn(null);
        OutsideWeatherSnapshot createdWeatherSnapshot = OutsideWeatherSnapshot.builder()
                .weatherSnapshotId(21L).nx(60).ny(127).windowStart(windowStart).build();
        when(outsideWeatherSnapshotService.createOutsideWeatherSnapshot(
                60, 127, windowStart, BigDecimal.valueOf(33.7), BigDecimal.valueOf(69)))
                .thenReturn(createdWeatherSnapshot);

        feedbackPersistenceService.persist(event(), referenceAt);

        verify(regionCacheService).save(7L, new RoomWeatherRegion(60, 127));
        verify(feedbackLogService).createFeedbackLogWithScores(
                eq(1L), eq(7L), any(), eq(createdWeatherSnapshot), eq("더워요"),
                any(), eq(false), isNull(), eq(List.of()));
    }

    @Test
    void persistSkipsWeatherSnapshotWhenBaseDateTimeIsUnparseable() {
        ZonedDateTime referenceAt = ZonedDateTime.now();
        when(roomEnvironmentSnapshotService.findSnapshotId(7L, referenceAt)).thenReturn(
                RoomEnvironmentSnapshot.builder().snapshotId(10L).roomId(7L).windowStart(referenceAt).build());
        when(regionCacheService.find(7L)).thenReturn(Optional.empty());
        OutsideWeather weather = new OutsideWeather("이상한형식", "33.7℃", "69%", 60, 127);
        when(coreApiClient.getOutsideWeather(7L, referenceAt.toLocalDateTime())).thenReturn(weather);

        feedbackPersistenceService.persist(event(), referenceAt);

        verify(outsideWeatherSnapshotService, never()).createOutsideWeatherSnapshot(any(), any(), any(), any(), any());
        verify(feedbackLogService).createFeedbackLogWithScores(
                eq(1L), eq(7L), any(), isNull(), eq("더워요"),
                any(), eq(false), isNull(), eq(List.of()));
    }
}
