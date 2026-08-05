package com.siren.notificationservice.core.entity.table;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutsideWeatherSnapshotTest {

    @Test
    void builderSetsAllFields() {
        ZonedDateTime windowStart = ZonedDateTime.now();

        OutsideWeatherSnapshot snapshot = OutsideWeatherSnapshot.builder()
                .nx(60)
                .ny(127)
                .windowStart(windowStart)
                .outsideTemperature(BigDecimal.valueOf(28.4))
                .outsideHumidity(BigDecimal.valueOf(63.0))
                .build();

        assertThat(snapshot.getNx()).isEqualTo(60);
        assertThat(snapshot.getNy()).isEqualTo(127);
        assertThat(snapshot.getWindowStart()).isEqualTo(windowStart);
        assertThat(snapshot.getOutsideTemperature()).isEqualByComparingTo("28.4");
        assertThat(snapshot.getOutsideHumidity()).isEqualByComparingTo("63.0");
    }
}
