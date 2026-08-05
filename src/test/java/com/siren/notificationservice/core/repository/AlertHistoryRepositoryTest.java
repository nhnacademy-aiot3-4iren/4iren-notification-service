package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class AlertHistoryRepositoryTest {

    @Autowired
    private AlertHistoryRepository alertHistoryRepository;

    @Test
    void savesAndFindsAlertHistoryById() {
        AlertHistory saved = alertHistoryRepository.save(AlertHistory.builder()
                .roomId(7L)
                .botType(BotType.ADMIN_BOT)
                .alertType(AlertType.SENSOR_ANOMALY)
                .message("센서 이상 감지")
                .sendAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .userId(1L)
                .build());

        Optional<AlertHistory> found = alertHistoryRepository.findById(saved.getAlertHistoryId());

        assertThat(found).isPresent();
        assertThat(found.get().getMessage()).isEqualTo("센서 이상 감지");
        assertThat(found.get().getAlertType()).isEqualTo(AlertType.SENSOR_ANOMALY);
    }

    @Test
    void findsEmptyWhenAlertHistoryDoesNotExist() {
        Optional<AlertHistory> found = alertHistoryRepository.findById(999L);

        assertThat(found).isEmpty();
    }
}
