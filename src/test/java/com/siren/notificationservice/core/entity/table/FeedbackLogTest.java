package com.siren.notificationservice.core.entity.table;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackLogTest {

    @Test
    void builderSetsAllFields() {
        ZonedDateTime createdAt = ZonedDateTime.now();
        ZonedDateTime experiencedAt = createdAt.minusHours(1);

        FeedbackLog feedbackLog = FeedbackLog.builder()
                .userId(1001L)
                .roomId(7L)
                .rawText("더워요")
                .createdAt(createdAt)
                .delayed(true)
                .experiencedAt(experiencedAt)
                .build();

        assertThat(feedbackLog.getUserId()).isEqualTo(1001L);
        assertThat(feedbackLog.getRoomId()).isEqualTo(7L);
        assertThat(feedbackLog.getRawText()).isEqualTo("더워요");
        assertThat(feedbackLog.getCreatedAt()).isEqualTo(createdAt);
        assertThat(feedbackLog.isDelayed()).isTrue();
        assertThat(feedbackLog.getExperiencedAt()).isEqualTo(experiencedAt);
        assertThat(feedbackLog.getSnapshot()).isNull();
        assertThat(feedbackLog.getOutsideWeatherSnapshot()).isNull();
    }
}
