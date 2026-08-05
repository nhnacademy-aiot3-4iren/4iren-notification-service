package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.FeedbackLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class FeedbackLogRepositoryTest {

    @Autowired
    private FeedbackLogRepository feedbackLogRepository;

    @Test
    void findsLogsAfterSinceIdInAscendingOrder() {
        Long firstId = feedbackLogRepository.save(feedbackLog()).getFeedbackLogId();
        Long secondId = feedbackLogRepository.save(feedbackLog()).getFeedbackLogId();
        Long thirdId = feedbackLogRepository.save(feedbackLog()).getFeedbackLogId();

        List<FeedbackLog> found = feedbackLogRepository
                .findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(firstId, PageRequest.of(0, 10));

        assertThat(found).extracting(FeedbackLog::getFeedbackLogId).containsExactly(secondId, thirdId);
    }

    @Test
    void limitsResultsByPageSize() {
        feedbackLogRepository.save(feedbackLog());
        feedbackLogRepository.save(feedbackLog());
        feedbackLogRepository.save(feedbackLog());

        List<FeedbackLog> found = feedbackLogRepository
                .findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(0L, PageRequest.of(0, 1));

        assertThat(found).hasSize(1);
    }

    private FeedbackLog feedbackLog() {
        return FeedbackLog.builder()
                .roomId(7L)
                .rawText("더워요")
                .createdAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .userId(1L)
                .delayed(false).build();
    }
}
