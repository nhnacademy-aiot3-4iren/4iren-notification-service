package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.service.FeedbackPersistenceService;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FeedbackProcessingListenerTest {

    private final FeedbackPersistenceService feedbackPersistenceService = mock(FeedbackPersistenceService.class);
    private final FeedbackProcessingListener feedbackProcessingListener = new FeedbackProcessingListener(feedbackPersistenceService);

    @Test
    void handleUsesExperiencedAtAsReferenceTimeWhenPresent() {
        ZonedDateTime experiencedAt = ZonedDateTime.now().minusHours(2);
        ZonedDateTime receivedAt = ZonedDateTime.now();
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "아까 더웠어요", List.of(), true, experiencedAt, receivedAt);

        feedbackProcessingListener.handle(event);

        verify(feedbackPersistenceService).persist(event, experiencedAt);
    }

    @Test
    void handleFallsBackToReceivedAtWhenExperiencedAtIsMissing() {
        ZonedDateTime receivedAt = ZonedDateTime.now();
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "더워요", List.of(), false, null, receivedAt);

        feedbackProcessingListener.handle(event);

        verify(feedbackPersistenceService).persist(event, receivedAt);
    }

    @Test
    void handleSwallowsExceptionSoMessageIsNotRequeuedForever() {
        ZonedDateTime receivedAt = ZonedDateTime.now();
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "더워요", List.of(), false, null, receivedAt);
        doThrow(new RuntimeException("DB 저장 실패")).when(feedbackPersistenceService).persist(event, receivedAt);

        feedbackProcessingListener.handle(event);

        // 예외가 여기까지 올라오지 않고 조용히 끝나야 한다 (DLQ 없는 구조라 무한 재큐잉 방지) -
        // handle()이 끝까지 실행돼서 persist가 실제로 호출됐다는 것 자체가 증거
        verify(feedbackPersistenceService).persist(event, receivedAt);
    }
}
