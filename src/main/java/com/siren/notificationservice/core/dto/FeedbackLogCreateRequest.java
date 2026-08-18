package com.siren.notificationservice.core.dto;

import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * FeedbackLog(+FeedbackScore) 생성에 필요한 값을 하나로 묶는다.
 * snapshot/outsideWeatherSnapshot/experiencedAt은 조회 실패 시 없을 수 있어 nullable, 나머지는 필수값이다.
 */
public record FeedbackLogCreateRequest(
        Long userId,
        Long roomId,
        RoomEnvironmentSnapshot snapshot,
        OutsideWeatherSnapshot outsideWeatherSnapshot,
        String rawText,
        LocalDateTime createdAt,
        boolean delayed,
        LocalDateTime experiencedAt,
        List<FeedbackExtractionResult.SensorScore> sensorScores
) {
    private static final String USER_ID_NULL_MESSAGE = "userId는 null일 수 없습니다.";
    private static final String ROOM_ID_NULL_MESSAGE = "roomId는 null일 수 없습니다.";
    private static final String RAW_TEXT_NULL_MESSAGE = "rawText는 null일 수 없습니다.";
    private static final String CREATED_AT_NULL_MESSAGE = "createdAt은 null일 수 없습니다.";
    private static final String SENSOR_SCORES_NULL_MESSAGE = "sensorScores는 null일 수 없습니다.";

    public FeedbackLogCreateRequest {
        Objects.requireNonNull(userId, USER_ID_NULL_MESSAGE);
        Objects.requireNonNull(roomId, ROOM_ID_NULL_MESSAGE);
        Objects.requireNonNull(rawText, RAW_TEXT_NULL_MESSAGE);
        Objects.requireNonNull(createdAt, CREATED_AT_NULL_MESSAGE);
        Objects.requireNonNull(sensorScores, SENSOR_SCORES_NULL_MESSAGE);
    }
}
