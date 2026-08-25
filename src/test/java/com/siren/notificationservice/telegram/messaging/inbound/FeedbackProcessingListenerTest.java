package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.service.FeedbackPersistenceService;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FeedbackProcessingListenerTest {

    private final FeedbackPersistenceService feedbackPersistenceService = mock(FeedbackPersistenceService.class);
    private final FeedbackProcessingListener feedbackProcessingListener = new FeedbackProcessingListener(feedbackPersistenceService);

    @Test
    void handleUsesExperiencedAtAsReferenceTimeWhenPresent() {
        LocalDateTime experiencedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime receivedAt = LocalDateTime.now();
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "아까 더웠어요", List.of(), true, experiencedAt, receivedAt);

        feedbackProcessingListener.handle(event);

        verify(feedbackPersistenceService).persist(event, experiencedAt);
    }

    @Test
    void handleFallsBackToReceivedAtWhenExperiencedAtIsMissing() {
        LocalDateTime receivedAt = LocalDateTime.now();
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "더워요", List.of(), false, null, receivedAt);

        feedbackProcessingListener.handle(event);

        verify(feedbackPersistenceService).persist(event, receivedAt);
    }

    @Test
    void handleLetsExceptionPropagateSoItReachesDlq() {
        LocalDateTime receivedAt = LocalDateTime.now();
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "더워요", List.of(), false, null, receivedAt);
        doThrow(new RuntimeException("DB 저장 실패")).when(feedbackPersistenceService).persist(event, receivedAt);

        // 예상 밖 실패는 삼키지 않고 던져야 재시도→DLQ로 흘러간다 (DLQ 도입 후 정책)
        assertThrows(RuntimeException.class, () -> feedbackProcessingListener.handle(event));
    }
}
