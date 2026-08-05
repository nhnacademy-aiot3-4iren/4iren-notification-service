package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.repository.FeedbackLogRepository;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackLogServiceTest {

    private final FeedbackLogRepository feedbackLogRepository = mock(FeedbackLogRepository.class);
    private final FeedbackScoreService feedbackScoreService = mock(FeedbackScoreService.class);
    private final FeedbackLogService feedbackLogService = new FeedbackLogService(feedbackLogRepository, feedbackScoreService);

    private FeedbackLog log(Long id) {
        return FeedbackLog.builder()
                .feedbackLogId(id)
                .roomId(7L)
                .rawText("더워요")
                .createdAt(ZonedDateTime.now())
                .userId(1L)
                .build();
    }

    @Test
    void getFeedbackLogReturnsLog() {
        FeedbackLog log = log(1L);
        when(feedbackLogRepository.findById(1L)).thenReturn(Optional.of(log));

        FeedbackLog result = feedbackLogService.getFeedbackLog(1L);

        assertThat(result).isEqualTo(log);
    }

    @Test
    void getFeedbackLogReturnsNullWhenNotFound() {
        when(feedbackLogRepository.findById(1L)).thenReturn(Optional.empty());

        FeedbackLog result = feedbackLogService.getFeedbackLog(1L);

        assertThat(result).isNull();
    }

    @Test
    void getFeedbackLogThrowsWhenIdIsNull() {
        assertThatThrownBy(() -> feedbackLogService.getFeedbackLog(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createFeedbackLogWithScoresSavesLogAndScores() {
        when(feedbackLogRepository.save(any())).thenReturn(log(1L));
        List<FeedbackExtractionResult.SensorScore> sensorScores =
                List.of(new FeedbackExtractionResult.SensorScore(SensorType.TEMPERATURE, 1));

        feedbackLogService.createFeedbackLogWithScores(1L, 7L, null, null, "더워요",
                ZonedDateTime.now(), false, null, sensorScores);

        verify(feedbackLogRepository).save(any());
        verify(feedbackScoreService).saveAll(any());
    }

    @Test
    void createFeedbackLogWithScoresThrowsWhenSensorScoresIsNull() {
        ZonedDateTime createdAt = ZonedDateTime.now();

        assertThatThrownBy(() -> feedbackLogService.createFeedbackLogWithScores(1L, 7L, null, null, "더워요",
                createdAt, false, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createFeedbackLogWithScoresThrowsWhenUserIdIsNull() {
        ZonedDateTime createdAt = ZonedDateTime.now();

        assertThatThrownBy(() -> feedbackLogService.createFeedbackLogWithScores(null, 7L, null, null, "더워요",
                createdAt, false, null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getFeedbackLogsReturnsLogsAfterSinceId() {
        FeedbackLog log = log(2L);
        when(feedbackLogRepository.findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(0L, PageRequest.of(0, 10)))
                .thenReturn(List.of(log));

        List<FeedbackLog> result = feedbackLogService.getFeedbackLogs(0L, 10);

        assertThat(result).containsExactly(log);
    }

    @Test
    void getFeedbackLogsCorrectsNegativeLimitToOne() {
        FeedbackLog log = log(2L);
        when(feedbackLogRepository.findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(0L, PageRequest.of(0, 1)))
                .thenReturn(List.of(log));

        List<FeedbackLog> result = feedbackLogService.getFeedbackLogs(0L, -5);

        assertThat(result).containsExactly(log);
    }

    @Test
    void getFeedbackLogsThrowsWhenSinceIdIsNull() {
        assertThatThrownBy(() -> feedbackLogService.getFeedbackLogs(null, 10))
                .isInstanceOf(NullPointerException.class);
    }
}
