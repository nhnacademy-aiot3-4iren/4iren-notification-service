package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class FeedbackScoreRepositoryTest {

    @Autowired
    private FeedbackScoreRepository feedbackScoreRepository;

    @Autowired
    private FeedbackLogRepository feedbackLogRepository;

    @Test
    void findsScoresByFeedbackLogId() {
        FeedbackLog log = feedbackLogRepository.save(feedbackLog());
        feedbackScoreRepository.save(score(log, SensorType.TEMPERATURE, 2));
        feedbackScoreRepository.save(score(log, SensorType.HUMIDITY, -1));

        List<FeedbackScore> found = feedbackScoreRepository.findById_FeedbackLogId(log.getFeedbackLogId());

        assertThat(found).hasSize(2);
    }

    @Test
    void findsScoresByFeedbackLogIdInBatch() {
        FeedbackLog log1 = feedbackLogRepository.save(feedbackLog());
        FeedbackLog log2 = feedbackLogRepository.save(feedbackLog());
        feedbackScoreRepository.save(score(log1, SensorType.TEMPERATURE, 2));
        feedbackScoreRepository.save(score(log2, SensorType.AIR_QUALITY, 1));

        List<FeedbackScore> found = feedbackScoreRepository
                .findById_FeedbackLogIdIn(List.of(log1.getFeedbackLogId(), log2.getFeedbackLogId()));

        assertThat(found).hasSize(2);
    }

    @Test
    void findsNothingWhenFeedbackLogHasNoScores() {
        FeedbackLog log = feedbackLogRepository.save(feedbackLog());

        List<FeedbackScore> found = feedbackScoreRepository.findById_FeedbackLogId(log.getFeedbackLogId());

        assertThat(found).isEmpty();
    }

    private FeedbackLog feedbackLog() {
        return FeedbackLog.builder()
                .roomId(7L)
                .rawText("더워요")
                .createdAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .userId(1L)
                .delayed(false)
                .build();
    }

    private FeedbackScore score(FeedbackLog log, SensorType sensorType, int value) {
        return FeedbackScore.builder()
                .id(FeedbackScoreId.builder().sensorType(sensorType).build())
                .feedbackLog(log)
                .score(value)
                .build();
    }
}
