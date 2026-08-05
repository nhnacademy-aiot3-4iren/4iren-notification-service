package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.repository.FeedbackScoreRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackScoreServiceTest {

    private final FeedbackScoreRepository feedbackScoreRepository = mock(FeedbackScoreRepository.class);
    private final FeedbackScoreService feedbackScoreService = new FeedbackScoreService(feedbackScoreRepository);

    private FeedbackScore score(Long feedbackLogId) {
        return FeedbackScore.builder()
                .id(FeedbackScoreId.builder().feedbackLogId(feedbackLogId).sensorType(SensorType.TEMPERATURE).build())
                .score(1)
                .build();
    }

    @Test
    void getScoresByFeedbackLogIdReturnsScores() {
        FeedbackScore score = score(1L);
        when(feedbackScoreRepository.findById_FeedbackLogId(1L)).thenReturn(List.of(score));

        List<FeedbackScore> result = feedbackScoreService.getScoresByFeedbackLogId(1L);

        assertThat(result).containsExactly(score);
    }

    @Test
    void getScoresByFeedbackLogIdThrowsWhenIdIsNull() {
        assertThatThrownBy(() -> feedbackScoreService.getScoresByFeedbackLogId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getScoresByFeedbackLogIdInReturnsScores() {
        FeedbackScore score = score(1L);
        when(feedbackScoreRepository.findById_FeedbackLogIdIn(List.of(1L, 2L))).thenReturn(List.of(score));

        List<FeedbackScore> result = feedbackScoreService.getScoresByFeedbackLogIdIn(List.of(1L, 2L));

        assertThat(result).containsExactly(score);
    }

    @Test
    void getScoresByFeedbackLogIdInThrowsWhenIdsIsNull() {
        assertThatThrownBy(() -> feedbackScoreService.getScoresByFeedbackLogIdIn(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void saveAllSavesScores() {
        List<FeedbackScore> scores = List.of(score(1L));

        feedbackScoreService.saveAll(scores);

        verify(feedbackScoreRepository).saveAll(scores);
    }

    @Test
    void saveAllThrowsWhenScoresIsNull() {
        assertThatThrownBy(() -> feedbackScoreService.saveAll(null))
                .isInstanceOf(NullPointerException.class);
    }
}
