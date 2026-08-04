package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RoomEnvironmentReadingTest {

    @Test
    void builderSetsAllFields() {
        RoomEnvironmentReadingId id = RoomEnvironmentReadingId.builder()
                .metricType(EnvironmentMetricType.TEMPERATURE)
                .build();

        RoomEnvironmentReading reading = RoomEnvironmentReading.builder()
                .id(id)
                .value(BigDecimal.valueOf(24.5))
                .build();

        assertThat(reading.getId().getMetricType()).isEqualTo(EnvironmentMetricType.TEMPERATURE);
        assertThat(reading.getValue()).isEqualByComparingTo("24.5");
    }
}
