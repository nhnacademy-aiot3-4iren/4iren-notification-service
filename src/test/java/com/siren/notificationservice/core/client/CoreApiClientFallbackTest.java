package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreApiClientFallbackTest {

    private final CoreApiClientFallback fallback = new CoreApiClientFallback();

    @Test
    void getRoomSubscriptionsThrowsError() {
        assertThatThrownBy(() -> fallback.getRoomSubscriptions(1L))
                .isInstanceOf(CoreApiUnavailableException.class)
                .hasMessageContaining("userId=1");
    }

    @Test
    void getRoomSensorsReadingsThrowsError() {
        LocalDateTime requestAt = LocalDateTime.now();

        assertThatThrownBy(() -> fallback.getRoomSensorsReadings(1L, requestAt))
                .isInstanceOf(CoreApiUnavailableException.class)
                .hasMessageContaining("roomId=1");
    }

    @Test
    void getOutsideWeatherThrowsError() {
        LocalDateTime requestAt = LocalDateTime.now();

        assertThatThrownBy(() -> fallback.getOutsideWeather(1L, requestAt))
                .isInstanceOf(CoreApiUnavailableException.class)
                .hasMessageContaining("roomId=1");
    }
}
