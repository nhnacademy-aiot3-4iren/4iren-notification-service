package com.siren.notificationservice.telegram.messaging.outbound;

import com.siren.notificationservice.core.config.properties.RabbitFeedbackProcessingProperties;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FeedbackProcessingEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitFeedbackProcessingProperties feedback = new RabbitFeedbackProcessingProperties();
    private final FeedbackProcessingEventPublisher publisher = new FeedbackProcessingEventPublisher(rabbitTemplate, feedback);

    @BeforeEach
    void setUp() {
        feedback.setExchange("notification.events");
        feedback.setRoutingKey("notification.feedback-processing");
    }

    @Test
    void publishSendsEventAndReturnsTrue() {
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "더워요", List.of(), false, null, LocalDateTime.now());

        boolean result = publisher.publish(event);

        assertThat(result).isTrue();
        verify(rabbitTemplate).convertAndSend("notification.events", "notification.feedback-processing", event);
    }

    @Test
    void publishReturnsFalseWhenRabbitTemplateFails() {
        FeedbackProcessingEvent event = new FeedbackProcessingEvent(1L, 7L, "더워요", List.of(), false, null, LocalDateTime.now());
        doThrow(new RuntimeException("브로커 연결 실패"))
                .when(rabbitTemplate).convertAndSend("notification.events", "notification.feedback-processing", event);

        boolean result = publisher.publish(event);

        assertThat(result).isFalse();
    }
}
