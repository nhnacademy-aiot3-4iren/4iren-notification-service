package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.EnrichedFeedbackLog;
import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.service.basic_service.FeedbackLogService;
import com.siren.notificationservice.core.service.basic_service.FeedbackScoreService;
import com.siren.notificationservice.core.service.basic_service.OutsideWeatherSnapshotService;
import com.siren.notificationservice.core.service.basic_service.RoomEnvironmentReadingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackExportQueryServiceTest {

    private final FeedbackLogService feedbackLogService = mock(FeedbackLogService.class);
    private final FeedbackScoreService feedbackScoreService = mock(FeedbackScoreService.class);
    private final RoomEnvironmentReadingService roomEnvironmentReadingService = mock(RoomEnvironmentReadingService.class);
    private final OutsideWeatherSnapshotService outsideWeatherSnapshotService = mock(OutsideWeatherSnapshotService.class);
    private final FeedbackExportQueryService feedbackExportQueryService = new FeedbackExportQueryService(
            feedbackLogService, feedbackScoreService, roomEnvironmentReadingService, outsideWeatherSnapshotService);

    @Test
    void fetchReturnsEmptyListWhenNoLogs() {
        when(feedbackLogService.getFeedbackLogs(0L, 10)).thenReturn(List.of());

        List<EnrichedFeedbackLog> result = feedbackExportQueryService.fetch(0L, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchBundlesLogWithItsScoresReadingsAndWeather() {
        RoomEnvironmentSnapshot snapshot = RoomEnvironmentSnapshot.builder()
                .snapshotId(100L).roomId(7L).windowStart(LocalDateTime.now()).build();
        OutsideWeatherSnapshot weather = OutsideWeatherSnapshot.builder()
                .weatherSnapshotId(200L).nx(60).ny(127).windowStart(LocalDateTime.now()).build();
        FeedbackLog log = FeedbackLog.builder()
                .feedbackLogId(1L).roomId(7L).snapshot(snapshot).outsideWeatherSnapshot(weather)
                .rawText("더워요").createdAt(LocalDateTime.now()).userId(1L).delayed(false).build();
        FeedbackScore score = FeedbackScore.builder()
                .id(FeedbackScoreId.builder().feedbackLogId(1L).sensorType(SensorType.TEMPERATURE).build())
                .score(1).build();
        RoomEnvironmentReading reading = RoomEnvironmentReading.builder()
                .id(RoomEnvironmentReadingId.builder().snapshotId(100L).metricType(EnvironmentMetricType.TEMPERATURE).build())
                .value(BigDecimal.valueOf(24.5)).build();

        when(feedbackLogService.getFeedbackLogs(0L, 10)).thenReturn(List.of(log));
        when(feedbackScoreService.getScoresByFeedbackLogIdIn(List.of(1L))).thenReturn(List.of(score));
        when(roomEnvironmentReadingService.getReadingsBySnapshotIds(List.of(100L))).thenReturn(List.of(reading));
        when(outsideWeatherSnapshotService.getByIds(List.of(200L))).thenReturn(List.of(weather));

        List<EnrichedFeedbackLog> result = feedbackExportQueryService.fetch(0L, 10);

        assertThat(result).hasSize(1);
        EnrichedFeedbackLog enriched = result.get(0);
        assertThat(enriched.feedbackLog()).isEqualTo(log);
        assertThat(enriched.scores()).containsExactly(score);
        assertThat(enriched.readings()).containsExactly(reading);
        assertThat(enriched.outsideWeatherSnapshot()).isEqualTo(weather);
    }

    @Test
    void fetchLeavesReadingsAndWeatherEmptyWhenLogHasNoSnapshot() {
        FeedbackLog log = FeedbackLog.builder()
                .feedbackLogId(1L).roomId(7L).rawText("더워요").createdAt(LocalDateTime.now()).delayed(false).userId(1L).build();

        when(feedbackLogService.getFeedbackLogs(0L, 10)).thenReturn(List.of(log));
        when(feedbackScoreService.getScoresByFeedbackLogIdIn(List.of(1L))).thenReturn(List.of());
        when(roomEnvironmentReadingService.getReadingsBySnapshotIds(List.of())).thenReturn(List.of());
        when(outsideWeatherSnapshotService.getByIds(List.of())).thenReturn(List.of());

        List<EnrichedFeedbackLog> result = feedbackExportQueryService.fetch(0L, 10);

        assertThat(result).hasSize(1);
        EnrichedFeedbackLog enriched = result.get(0);
        assertThat(enriched.readings()).isEmpty();
        assertThat(enriched.outsideWeatherSnapshot()).isNull();
    }

    @Test
    void fetchThrowsWhenSinceIdIsNull() {
        assertThatThrownBy(() -> feedbackExportQueryService.fetch(null, 10))
                .isInstanceOf(NullPointerException.class);
    }
}
