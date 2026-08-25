package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.dto.request.AlertHistorySearchCondition;
import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalDateTime;
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

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    private AlertHistoryRepository alertHistoryRepository;

    private AlertHistory save(Long userId, BotType botType, AlertType alertType, LocalDateTime sendAt, String eventId) {
        return alertHistoryRepository.save(AlertHistory.builder()
                .roomId(7L)
                .botType(botType)
                .alertType(alertType)
                .eventId(eventId)
                .message("msg")
                .sendAt(sendAt.truncatedTo(ChronoUnit.SECONDS))
                .userId(userId)
                .build());
    }

    @Test
    void savesAndFindsAlertHistoryById() {
        AlertHistory saved = save(1L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, LocalDateTime.now(ZONE), "evt-1");

        Optional<AlertHistory> found = alertHistoryRepository.findById(saved.getAlertHistoryId());

        assertThat(found).isPresent();
        assertThat(found.get().getMessage()).isEqualTo("msg");
        assertThat(found.get().getAlertType()).isEqualTo(AlertType.SENSOR_ANOMALY);
    }

    @Test
    void findsEmptyWhenAlertHistoryDoesNotExist() {
        Optional<AlertHistory> found = alertHistoryRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void searchReturnsOnlyOwnHistory() {
        save(100L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, LocalDateTime.now(ZONE), "e1");
        save(999L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, LocalDateTime.now(ZONE), "e2"); // 남의 이력

        Page<AlertHistory> page = alertHistoryRepository.search(
                100L, new AlertHistorySearchCondition(null, null, null, null, null), PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(AlertHistory::getUserId).containsOnly(100L);
    }

    @Test
    void searchFiltersByAlertType() {
        save(100L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, LocalDateTime.now(ZONE), "e1");
        save(100L, BotType.ADMIN_BOT, AlertType.VENTILATION_RECOMMEND, LocalDateTime.now(ZONE), "e2");

        Page<AlertHistory> page = alertHistoryRepository.search(
                100L, new AlertHistorySearchCondition(null, null, "SENSOR_ANOMALY", null, null), PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(AlertHistory::getAlertType).containsOnly(AlertType.SENSOR_ANOMALY);
    }

    @Test
    void searchFiltersByBotType() {
        save(100L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, LocalDateTime.now(ZONE), "e1");
        save(100L, BotType.USER_BOT, AlertType.SENSOR_ANOMALY, LocalDateTime.now(ZONE), "e2");

        Page<AlertHistory> page = alertHistoryRepository.search(
                100L, new AlertHistorySearchCondition(null, "USER_BOT", null, null, null), PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(AlertHistory::getBotType).containsOnly(BotType.USER_BOT);
    }

    @Test
    void searchFiltersByDateRange_fromExcludesOlder_toIncludesToDay() {
        LocalDate today = LocalDate.now(ZONE); // 한 번만 캡처해 자정 경계 flaky 제거
        save(100L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, today.minusDays(10).atTime(12, 0), "e-old");
        save(100L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, today.atTime(12, 0), "e-today");
        save(100L, BotType.ADMIN_BOT, AlertType.SENSOR_ANOMALY, today.plusDays(1).atTime(12, 0), "e-tomorrow");

        // from=today(오래된 것 제외) + to=today(다음날 제외, to 당일은 포함) -> today 것만
        Page<AlertHistory> page = alertHistoryRepository.search(
                100L, new AlertHistorySearchCondition(null, null, null, today, today), PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(AlertHistory::getEventId).containsExactly("e-today");
    }
}
