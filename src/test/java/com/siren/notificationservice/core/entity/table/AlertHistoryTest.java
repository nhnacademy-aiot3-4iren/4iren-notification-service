package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AlertHistoryTest {

    @Test
    void builderSetsAllFields() {
        ZonedDateTime sentAt = ZonedDateTime.now();

        AlertHistory alertHistory = AlertHistory.builder()
                .roomId(7L)
                .botType(BotType.ADMIN_BOT)
                .alertType(AlertType.SENSOR_ANOMALY)
                .message("센서 이상이 감지됐습니다")
                .sendAt(sentAt)
                .userId(1001L)
                .build();

        assertThat(alertHistory.getRoomId()).isEqualTo(7L);
        assertThat(alertHistory.getBotType()).isEqualTo(BotType.ADMIN_BOT);
        assertThat(alertHistory.getAlertType()).isEqualTo(AlertType.SENSOR_ANOMALY);
        assertThat(alertHistory.getMessage()).isEqualTo("센서 이상이 감지됐습니다");
        assertThat(alertHistory.getSendAt()).isEqualTo(sentAt);
        assertThat(alertHistory.getUserId()).isEqualTo(1001L);
    }
}
