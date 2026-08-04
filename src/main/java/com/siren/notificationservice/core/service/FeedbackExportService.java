package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.response.FeedbackExportResult;
import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.table.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackExportService {

    private static final String HEADER = "feedback_log_id,room_id,reference_at,metric_type,value\n";

    private final FeedbackLogService feedbackLogService;
    private final FeedbackScoreService feedbackScoreService;
    private final RoomEnvironmentReadingService roomEnvironmentReadingService;

    /**
     * sinceId 이후의 피드백을 최대 limit건 조회해서 CSV(long/key-value 형식)로 조립한다.
     * 점수/스냅샷이 하나도 없는 feedback_log는 CSV row가 0개일 수 있어서, 다음 페이지
     * 커서로 쓸 lastFeedbackLogId(이번에 실제로 조회한 마지막 id)를 CSV와 별도로 반환한다.
     */
    @Transactional(readOnly = true)
    public FeedbackExportResult exportAsCsv(Long sinceId, int limit) {
        List<FeedbackLog> logs = feedbackLogService.getFeedbackLogs(sinceId, limit);

        StringBuilder csv = new StringBuilder(HEADER);
        Long lastFeedbackLogId = sinceId;
        for (FeedbackLog log : logs) {
            String referenceAt = resolveReferenceAt(log);
            for (String row : buildRowToFeedbackLog(log, referenceAt)) {
                csv.append(row);
            }
            lastFeedbackLogId = log.getFeedbackLogId();
        }
        return new FeedbackExportResult(csv.toString(), lastFeedbackLogId);
    }

    /**
     * 한 피드백 당 여러 행 만드는 거
     * feedback_log_id,room_id,reference_at,metric_type,value
     * 1042,7,2026-07-20T14:00:00+09:00,FEEDBACK_TEMPERATURE,1
     * 1042,7,2026-07-20T14:00:00+09:00,FEEDBACK_HUMIDITY,-1
     * 1042,7,2026-07-20T14:00:00+09:00,OUTSIDE_TEMPERATURE,28.4
     * 1042,7,2026-07-20T14:00:00+09:00,OUTSIDE_HUMIDITY,63.0
     * 1042,7,2026-07-20T14:00:00+09:00,INDOOR_OUTDOOR_TEMP_DIFF,-4.2
     * 1042,7,2026-07-20T14:00:00+09:00,INDOOR_OUTDOOR_HUMIDITY_DIFF,3.5
     */
    private List<String> buildRowToFeedbackLog(FeedbackLog log, String referenceAt) {
        List<String> rows = new ArrayList<>();

        List<FeedbackScore> scores = feedbackScoreService.getScoresByFeedbackLogId(log.getFeedbackLogId());
        for (FeedbackScore score : scores) {
            String metricType = "FEEDBACK_"+score.getId().getSensorType().name();
            rows.add(csvRow(log,referenceAt,metricType, score.getScore()));
        }

        // 실내값 센서
        BigDecimal indoorTemp = extractIndoor(log.getSnapshot(), EnvironmentMetricType.TEMPERATURE);
        BigDecimal indoorHumidity = extractIndoor(log.getSnapshot(), EnvironmentMetricType.HUMIDITY);

        OutsideWeatherSnapshot outside =log.getOutsideWeatherSnapshot();

        if(outside != null) {
            if (outside.getOutsideTemperature() != null) {
                rows.add(csvRow(log, referenceAt, "OUTSIDE_TEMPERATURE", outside.getOutsideTemperature()));
            }
            if (outside.getOutsideHumidity() != null) {
                rows.add(csvRow(log, referenceAt, "OUTSIDE_HUMIDITY", outside.getOutsideHumidity()));
            }
            if (indoorTemp != null && outside.getOutsideTemperature() != null) {
                rows.add(csvRow(log, referenceAt, "INDOOR_OUTDOOR_TEMP_DIFF", indoorTemp.subtract(outside.getOutsideTemperature())));
            }
            if (indoorHumidity != null && outside.getOutsideHumidity() != null) {
                rows.add(csvRow(log, referenceAt, "INDOOR_OUTDOOR_HUMIDITY_DIFF", indoorHumidity.subtract(outside.getOutsideHumidity())));
            }
        }
        return rows;
    }

    private String csvRow(FeedbackLog log, String referenceAt, String metricType, Number value) {
        String valueStr = value instanceof BigDecimal bd ? bd.toPlainString() : String.valueOf(value);
        return String.join(",", String.valueOf(log.getFeedbackLogId()), String.valueOf(log.getRoomId()), referenceAt, metricType, valueStr)+"\n";
    }

    private String resolveReferenceAt (FeedbackLog log) {
        if(log.isDelayed() && log.getExperiencedAt() == null) return "";
        ZonedDateTime referenceAt = log.getExperiencedAt() != null ? log.getExperiencedAt() : log.getCreatedAt();
        return referenceAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private BigDecimal extractIndoor(RoomEnvironmentSnapshot snapshot, EnvironmentMetricType metricType) {
        if(snapshot==null )return null;
        return roomEnvironmentReadingService.getReadingsBySnapshotId(snapshot.getSnapshotId()).stream()
                .filter(r -> r.getId().getMetricType() == metricType)
                .map(RoomEnvironmentReading::getValue)
                .findFirst().orElse(null);
    }
}
