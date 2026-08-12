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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackCsvConverterTest {

    private final FeedbackCsvConverter feedbackCsvConverter = new FeedbackCsvConverter();

    @Test
    void toCsvStartsWithHeaderEvenWhenNoLogs() {
        String csv = feedbackCsvConverter.toCsv(List.of());

        assertThat(csv).isEqualTo("feedback_log_id,room_id,reference_at,metric_type,value\n");
    }

    @Test
    void toCsvWritesFeedbackScoreAndWeatherDiffRows() {
        ZonedDateTime experiencedAt = ZonedDateTime.parse("2026-07-20T14:00:00+09:00");
        FeedbackLog log = FeedbackLog.builder()
                .feedbackLogId(1042L).roomId(7L).rawText("더워요")
                .createdAt(experiencedAt).experiencedAt(experiencedAt).userId(1L).delayed(false).build();
        FeedbackScore score = FeedbackScore.builder()
                .id(FeedbackScoreId.builder().feedbackLogId(1042L).sensorType(SensorType.TEMPERATURE).build())
                .score(1).build();
        RoomEnvironmentReading indoorTemperature = RoomEnvironmentReading.builder()
                .id(RoomEnvironmentReadingId.builder().snapshotId(100L).metricType(EnvironmentMetricType.TEMPERATURE).build())
                .value(BigDecimal.valueOf(24.2)).build();
        OutsideWeatherSnapshot weather = OutsideWeatherSnapshot.builder()
                .weatherSnapshotId(200L).nx(60).ny(127).windowStart(experiencedAt)
                .outsideTemperature(BigDecimal.valueOf(28.4)).build();
        EnrichedFeedbackLog enriched = new EnrichedFeedbackLog(log, List.of(score), List.of(indoorTemperature), weather);

        String csv = feedbackCsvConverter.toCsv(List.of(enriched));

        assertThat(csv)
                .contains("1042,7,2026-07-20T14:00:00+09:00,FEEDBACK_TEMPERATURE,1")
                .contains("1042,7,2026-07-20T14:00:00+09:00,OUTSIDE_TEMPERATURE,28.4")
                .contains("1042,7,2026-07-20T14:00:00+09:00,INDOOR_OUTDOOR_TEMP_DIFF,-4.2");
    }

    @Test
    void toCsvLeavesReferenceAtBlankWhenDelayedWithoutExperiencedTime() {
        FeedbackLog log = FeedbackLog.builder()
                .feedbackLogId(1L).roomId(7L).rawText("아까 더웠어요")
                .createdAt(ZonedDateTime.now()).delayed(true).experiencedAt(null).userId(1L).build();
        EnrichedFeedbackLog enriched = new EnrichedFeedbackLog(log, List.of(), List.of(), null);

        String csv = feedbackCsvConverter.toCsv(List.of(enriched));

        assertThat(csv).isEqualTo("feedback_log_id,room_id,reference_at,metric_type,value\n");
    }

    @Test
    void toCsvSkipsWeatherRowsWhenNoOutsideWeather() {
        FeedbackLog log = FeedbackLog.builder()
                .feedbackLogId(1L).roomId(7L).rawText("더워요")
                .createdAt(ZonedDateTime.now()).userId(1L).delayed(false).build();
        FeedbackScore score = FeedbackScore.builder()
                .id(FeedbackScoreId.builder().feedbackLogId(1L).sensorType(SensorType.HUMIDITY).build())
                .score(-1).build();
        EnrichedFeedbackLog enriched = new EnrichedFeedbackLog(log, List.of(score), List.of(), null);

        String csv = feedbackCsvConverter.toCsv(List.of(enriched));

        assertThat(csv)
                .contains("FEEDBACK_HUMIDITY,-1")
                .doesNotContain("OUTSIDE_")
                .doesNotContain("INDOOR_OUTDOOR_");
    }
}
