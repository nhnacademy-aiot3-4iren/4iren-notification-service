package com.siren.notificationservice.telegram.dto.feedback;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExperiencedTimeResolverTest {

    private FeedbackExtractionResult result(Integer hour, String meridiem, Integer minute) {
        return new FeedbackExtractionResult(List.of(), false, hour, meridiem, minute, null);
    }

    @Test
    void resolvesAfternoonHourTo24HourClock() {
        ZonedDateTime receivedAt = ZonedDateTime.parse("2026-07-20T20:00:00+09:00");

        ZonedDateTime resolved = ExperiencedTimeResolver.resolve(result(2, "PM", 30), receivedAt);

        assertThat(resolved.getHour()).isEqualTo(14);
        assertThat(resolved.getMinute()).isEqualTo(30);
        assertThat(resolved.toLocalDate()).isEqualTo(receivedAt.toLocalDate());
    }

    @Test
    void resolvesMorningTwelveToMidnight() {
        ZonedDateTime resolved = ExperiencedTimeResolver.resolve(result(12, "AM", 0), ZonedDateTime.now());

        assertThat(resolved.getHour()).isZero();
    }

    @Test
    void resolvesAfternoonTwelveToNoon() {
        ZonedDateTime resolved = ExperiencedTimeResolver.resolve(result(12, "PM", 0), ZonedDateTime.now());

        assertThat(resolved.getHour()).isEqualTo(12);
    }

    @Test
    void defaultsMinuteToZeroWhenNotMentioned() {
        ZonedDateTime resolved = ExperiencedTimeResolver.resolve(result(3, "PM", null), ZonedDateTime.now());

        assertThat(resolved.getMinute()).isZero();
    }

    @Test
    void returnsNullWhenHourIsMissing() {
        ZonedDateTime resolved = ExperiencedTimeResolver.resolve(result(null, "PM", 0), ZonedDateTime.now());

        assertThat(resolved).isNull();
    }

    @Test
    void returnsNullWhenMeridiemIsMissing() {
        ZonedDateTime resolved = ExperiencedTimeResolver.resolve(result(3, null, 0), ZonedDateTime.now());

        assertThat(resolved).isNull();
    }
}
