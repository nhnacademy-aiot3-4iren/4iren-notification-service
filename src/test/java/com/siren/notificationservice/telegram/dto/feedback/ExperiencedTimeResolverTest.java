package com.siren.notificationservice.telegram.dto.feedback;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExperiencedTimeResolverTest {

    private FeedbackExtractionResult result(Integer hour, String meridiem, Integer minute) {
        return new FeedbackExtractionResult(List.of(), false, hour, meridiem, minute, null);
    }

    @Test
    void resolvesAfternoonHourTo24HourClock() {
        LocalDateTime receivedAt = LocalDateTime.parse("2026-07-20T20:00:00");

        LocalDateTime resolved = ExperiencedTimeResolver.resolve(result(2, "PM", 30), receivedAt);

        assertThat(resolved.getHour()).isEqualTo(14);
        assertThat(resolved.getMinute()).isEqualTo(30);
        assertThat(resolved.toLocalDate()).isEqualTo(receivedAt.toLocalDate());
    }

    @Test
    void resolvesMorningTwelveToMidnight() {
        LocalDateTime resolved = ExperiencedTimeResolver.resolve(result(12, "AM", 0), LocalDateTime.now());

        assertThat(resolved.getHour()).isZero();
    }

    @Test
    void resolvesAfternoonTwelveToNoon() {
        LocalDateTime resolved = ExperiencedTimeResolver.resolve(result(12, "PM", 0), LocalDateTime.now());

        assertThat(resolved.getHour()).isEqualTo(12);
    }

    @Test
    void defaultsMinuteToZeroWhenNotMentioned() {
        LocalDateTime resolved = ExperiencedTimeResolver.resolve(result(3, "PM", null), LocalDateTime.now());

        assertThat(resolved.getMinute()).isZero();
    }

    @Test
    void returnsNullWhenHourIsMissing() {
        LocalDateTime resolved = ExperiencedTimeResolver.resolve(result(null, "PM", 0), LocalDateTime.now());

        assertThat(resolved).isNull();
    }

    @Test
    void returnsNullWhenMeridiemIsMissing() {
        LocalDateTime resolved = ExperiencedTimeResolver.resolve(result(3, null, 0), LocalDateTime.now());

        assertThat(resolved).isNull();
    }
}
