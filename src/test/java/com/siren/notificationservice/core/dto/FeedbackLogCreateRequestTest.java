package com.siren.notificationservice.core.dto;

import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackLogCreateRequestTest {

    private final LocalDateTime createdAt = LocalDateTime.now();
    private final List<FeedbackExtractionResult.SensorScore> sensorScores =
            List.of(new FeedbackExtractionResult.SensorScore(SensorType.TEMPERATURE, 1));

    @Test
    void throwsWhenUserIdIsNull() {
        assertThatThrownBy(() -> new FeedbackLogCreateRequest(
                null, 7L, null, null, "더워요", createdAt, false, null, sensorScores))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsWhenRoomIdIsNull() {
        assertThatThrownBy(() -> new FeedbackLogCreateRequest(
                1L, null, null, null, "더워요", createdAt, false, null, sensorScores))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsWhenRawTextIsNull() {
        assertThatThrownBy(() -> new FeedbackLogCreateRequest(
                1L, 7L, null, null, null, createdAt, false, null, sensorScores))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsWhenCreatedAtIsNull() {
        assertThatThrownBy(() -> new FeedbackLogCreateRequest(
                1L, 7L, null, null, "더워요", null, false, null, sensorScores))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsWhenSensorScoresIsNull() {
        assertThatThrownBy(() -> new FeedbackLogCreateRequest(
                1L, 7L, null, null, "더워요", createdAt, false, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
