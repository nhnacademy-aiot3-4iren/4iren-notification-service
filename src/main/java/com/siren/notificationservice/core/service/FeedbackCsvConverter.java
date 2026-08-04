package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.EnrichedFeedbackLog;
import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * EnrichedFeedbackLog 목록을 long(key-value) 형식 CSV 문자열로 변환한다. DB 접근이 전혀
 * 없는 순수 변환 로직이라 트랜잭션이 필요 없다.
 */
@Component
public class FeedbackCsvConverter {
    /// AI Learning 팀과 합의한 CSV 계약임
    /// 변경 전 AI Learning팀과 협의해야함
    private static final String HEADER = "feedback_log_id,room_id,reference_at,metric_type,value\n";
    private static final String OUTSIDE_TEMPERATURE = "OUTSIDE_TEMPERATURE";
    private static final String OUTSIDE_HUMIDITY = "OUTSIDE_HUMIDITY";
    private static final String INDOOR_OUTDOOR_TEMP_DIFF = "INDOOR_OUTDOOR_TEMP_DIFF";
    private static final String INDOOR_OUTDOOR_HUMIDITY_DIFF = "INDOOR_OUTDOOR_HUMIDITY_DIFF";
    private static final String FEEDBACK_METRIC_PREFIX = "FEEDBACK_";

    /**
     * 헤더를 포함한 CSV 전체 문자열을 만든다.
     */
    public String toCsv(List<EnrichedFeedbackLog> logs) {
        StringBuilder csv = new StringBuilder(HEADER);
        for (EnrichedFeedbackLog enrichedLog : logs) {
            for (String row : buildRows(enrichedLog)) {
                csv.append(row);
            }
        }
        return csv.toString();
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
    private List<String> buildRows(EnrichedFeedbackLog enrichedLog) {
        FeedbackLog log = enrichedLog.feedbackLog();
        String referenceAt = resolveReferenceAt(log);

        List<String> rows = new java.util.ArrayList<>();
        for (FeedbackScore score : enrichedLog.scores()) {
            rows.add(csvRow(log, referenceAt, feedbackMetricType(score.getId().getSensorType()), score.getScore()));
        }

        BigDecimal indoorTemperature = extractIndoor(enrichedLog.readings(), EnvironmentMetricType.TEMPERATURE);
        BigDecimal indoorHumidity = extractIndoor(enrichedLog.readings(), EnvironmentMetricType.HUMIDITY);

        OutsideWeatherSnapshot outside = enrichedLog.outsideWeatherSnapshot();
        if (outside != null) {
            if (outside.getOutsideTemperature() != null) {
                rows.add(csvRow(log, referenceAt, OUTSIDE_TEMPERATURE, outside.getOutsideTemperature()));
            }
            if (outside.getOutsideHumidity() != null) {
                rows.add(csvRow(log, referenceAt, OUTSIDE_HUMIDITY, outside.getOutsideHumidity()));
            }
            if (indoorTemperature != null && outside.getOutsideTemperature() != null) {
                rows.add(csvRow(log, referenceAt, INDOOR_OUTDOOR_TEMP_DIFF, indoorTemperature.subtract(outside.getOutsideTemperature())));
            }
            if (indoorHumidity != null && outside.getOutsideHumidity() != null) {
                rows.add(csvRow(log, referenceAt, INDOOR_OUTDOOR_HUMIDITY_DIFF, indoorHumidity.subtract(outside.getOutsideHumidity())));
            }
        }
        return rows;
    }

    /**
     * feedback_score의 sensorType을 CSV의 metric_type 문자열로 조합하는 규칙을 여기 한 곳에서만 관리한다.
     */
    private String feedbackMetricType(SensorType sensorType) {
        return FEEDBACK_METRIC_PREFIX + sensorType.name();
    }

    private String csvRow(FeedbackLog log, String referenceAt, String metricType, Number value) {
        String valueText = value instanceof BigDecimal bigDecimal ? bigDecimal.toPlainString() : String.valueOf(value);
        return String.join(",", String.valueOf(log.getFeedbackLogId()), String.valueOf(log.getRoomId()), referenceAt, metricType, valueText) + "\n";
    }

    /**
     * 지연 제출인데 구체적 체감 시각을 못 얻은 경우(신뢰 불가) 빈 값을 반환한다.
     */
    private String resolveReferenceAt(FeedbackLog log) {
        if (log.isDelayed() && log.getExperiencedAt() == null) {
            return "";
        }
        ZonedDateTime referenceAt = log.getExperiencedAt() != null ? log.getExperiencedAt() : log.getCreatedAt();
        return referenceAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private BigDecimal extractIndoor(List<RoomEnvironmentReading> readings, EnvironmentMetricType metricType) {
        return readings.stream()
                .filter(reading -> reading.getId().getMetricType() == metricType)
                .map(RoomEnvironmentReading::getValue)
                .findFirst()
                .orElse(null);
    }
}
