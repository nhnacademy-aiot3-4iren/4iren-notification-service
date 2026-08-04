package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import com.siren.notificationservice.core.entity.domain.SensorType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackScoreTest {

    @Test
    void builderSetsAllFields() {
        FeedbackScoreId id = FeedbackScoreId.builder()
                .sensorType(SensorType.TEMPERATURE)
                .build();

        FeedbackScore score = FeedbackScore.builder()
                .id(id)
                .score(2)
                .build();

        assertThat(score.getId().getSensorType()).isEqualTo(SensorType.TEMPERATURE);
        assertThat(score.getScore()).isEqualTo(2);
    }
}
