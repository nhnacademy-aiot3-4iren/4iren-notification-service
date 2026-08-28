package com.siren.notificationservice.telegram.messaging.outbound;

import com.siren.notificationservice.core.config.properties.RabbitFeedbackProcessingProperties;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackProcessingEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final RabbitFeedbackProcessingProperties feedback;

    public boolean publish(FeedbackProcessingEvent event) {
        try {
            rabbitTemplate.convertAndSend(feedback.getExchange(), feedback.getRoutingKey(), event);
            return true;
        } catch (Exception e) {
            log.warn("[FeedbackProcessingEventPublisher] publish 실패 (event={})", event, e);
            return false;
        }
    }
}
