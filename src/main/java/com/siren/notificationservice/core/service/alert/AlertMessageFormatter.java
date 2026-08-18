package com.siren.notificationservice.core.service.alert;

import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.dto.event.Operator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AlertEvent를 사람이 읽을 자연어 메시지로 변환함.
 * 필드가 있으면 노출하고 없으면 생략함
 * 수신자/긴급도 판단은 하지 않음.
 */
@Component
public class AlertMessageFormatter {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DETECTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 단건 && 긴급
    public String format(AlertEvent event, String roomName) {
        List<String> lines = Arrays.asList(
                event.alertTitle(),
                formatRoomName(roomName),
                formatDevicePoint(event.point()),
                formatDetectedAt(event.detectedAt()),
                formatDeviceEui(event.deviceEui(), event.alertType().isUrgent()),
                formatDeviceName(event.deviceName(), event.alertType().isUrgent()),
                buildNodeSection(event.nodeResults())
        );
        return lines.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    // 1개 이상 && 비긴급
    public String formatDigest(List<AlertDigestBufferEntry> entries){
        if(entries == null || entries.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("📋 알림 브리핑(").append(entries.size()).append("건)\n");
        Map<String, List<AlertDigestBufferEntry>> byRoom = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.roomName() != null ? entry.roomName() : "기타",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        byRoom.forEach((roomName, roomEntry) -> {
            sb.append("\n📍 ").append(roomName);

            for(AlertDigestBufferEntry entry : roomEntry) {
                sb.append("\n ").append(format(entry.event(), null));
            }
            sb.append("\n");
        });
        return sb.toString();
    }

    private String formatDetectedAt(Instant detectedAt) {
        return "🕐 탐지 " + LocalDateTime.ofInstant(detectedAt, SERVICE_ZONE).format(DETECTED_AT_FORMAT);
    }

    private String formatRoomName(String roomName) {
        return roomName != null ? "\n📍 " + roomName : null;
    }

    private String formatDevicePoint(String devicePoint) {
        return devicePoint != null ? "📍 " + devicePoint : null;
    }

    /**
     * deviceEui/deviceName은 Member에게 노출하면 안 되는 내부 식별 정보라 긴급(Admin 전용) 발송일 때만 보여준다.
     */
    private String formatDeviceEui(String deviceEui, boolean isUrgent) {
        return (isUrgent && deviceEui != null) ? "⚙️ Device: " + deviceEui : null;
    }

    private String formatDeviceName(String deviceName, boolean isUrgent) {
        return (isUrgent && deviceName != null) ? "⚙️ DeviceName: " + deviceName : null;
    }

    private String buildNodeSection(List<AlertEvent.NodeResult> nodeResults) {
        if (nodeResults == null || nodeResults.isEmpty()) return null;

        String joined = nodeResults.stream()
                .map(this::buildNodeClause)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));

        return joined.isBlank() ? null : joined;
    }

    private String buildNodeClause(AlertEvent.NodeResult nodeResult) {
        if (nodeResult.metricType() == null && nodeResult.value() == null) return null;

        StringBuilder sb = new StringBuilder();
        if (nodeResult.metricType() != null) {
            sb.append(nodeResult.metricType());
        }
        if (nodeResult.value() != null) {
            if (!sb.isEmpty()) {
                sb.append(" : ");
            }
            sb.append(nodeResult.value());
            if (nodeResult.unit() != null) {
                sb.append(nodeResult.unit());
            }
        }
        if (nodeResult.operator() != null && nodeResult.threshold() != null) {
            sb.append(" — 기준 ").append(nodeResult.threshold());
            if (nodeResult.unit() != null) {
                sb.append(nodeResult.unit());
            }
            sb.append(" ").append(Operator.labelOf(nodeResult.operator()));
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}
