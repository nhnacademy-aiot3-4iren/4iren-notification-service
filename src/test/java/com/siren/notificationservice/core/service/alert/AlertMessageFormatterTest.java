package com.siren.notificationservice.core.service.alert;

import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.dto.event.Operator;
import com.siren.notificationservice.core.entity.domain.AlertType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertMessageFormatterTest {

    private final AlertMessageFormatter formatter = new AlertMessageFormatter();

    private AlertEvent.NodeResult thresholdNode() {
        return new AlertEvent.NodeResult("THRESHOLD", "CO2", Operator.GT, "ppm", 1000.0, 1250.0);
    }

    private AlertEvent event(AlertType alertType, String title, String eventId) {
        return new AlertEvent(305L, alertType, title, "EUI-1", "CO2센서", null,
                List.of(thresholdNode()), Instant.parse("2026-08-06T09:30:00Z"), eventId);
    }

    @Test
    void formatUrgentShowsDeviceInfoAndThreshold() {
        String msg = formatter.format(event(AlertType.COMFORT_LIMIT_EXCEEDED, "CO2 위험 초과", "evt-1"), "305호");

        assertThat(msg)
                .contains("CO2 위험 초과")
                .contains("305호")
                .contains("CO2 : 1250.0ppm")
                .contains("기준 1000.0ppm 초과")
                .contains("EUI-1")
                .contains("CO2센서");
    }

    @Test
    void formatNonUrgentHidesDeviceInfo() {
        String msg = formatter.format(event(AlertType.VENTILATION_RECOMMEND, "환기 권장", "evt-2"), "305호");

        assertThat(msg)
                .contains("환기 권장")
                .doesNotContain("EUI-1")
                .doesNotContain("CO2센서");
    }

    @Test
    void formatOmitsThresholdWhenOperatorOrThresholdMissing() {
        AlertEvent anomaly = new AlertEvent(201L, AlertType.SENSOR_ANOMALY, "온도 이상치", "EUI-9", "온습도센서", null,
                List.of(new AlertEvent.NodeResult(null, "TEMPERATURE", null, "℃", null, 1324.0)),
                Instant.parse("2026-08-06T00:15:00Z"), "evt-3");

        String msg = formatter.format(anomaly, "201호");

        assertThat(msg)
                .contains("TEMPERATURE : 1324.0℃")
                .doesNotContain("기준");
    }

    @Test
    void formatDigestGroupsByRoomWithCount() {
        List<AlertDigestBufferEntry> entries = List.of(
                new AlertDigestBufferEntry(digestEvent("CO2 주의", "evt-a"), "305호"),
                new AlertDigestBufferEntry(digestEvent("습도 이상", "evt-b"), "305호"),
                new AlertDigestBufferEntry(digestEvent("온도 주의", "evt-c"), "201호")
        );

        String msg = formatter.formatDigest(entries);

        assertThat(msg)
                .contains("(3건)")
                .contains("📍 305호")
                .contains("📍 201호")
                .contains("CO2 주의")
                .contains("습도 이상")
                .contains("온도 주의");
    }

    @Test
    void formatDigestReturnsEmptyForEmptyList() {
        assertThat(formatter.formatDigest(List.of())).isEmpty();
    }

    private AlertEvent digestEvent(String title, String eventId) {
        return new AlertEvent(0L, AlertType.VENTILATION_RECOMMEND, title, null, null, null,
                List.of(thresholdNode()), Instant.parse("2026-08-06T09:30:00Z"), eventId);
    }
}
