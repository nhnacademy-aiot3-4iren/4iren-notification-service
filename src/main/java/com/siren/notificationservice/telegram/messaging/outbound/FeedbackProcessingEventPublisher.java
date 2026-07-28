package com.siren.notificationservice.telegram.messaging.outbound;

import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackProcessingEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notification-internal}")
    private String exchangeName;
    @Value("${rabbitmq.routing-key.feedback-processing}")
    private String routingKey;

    public boolean publish(FeedbackProcessingEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            return true;
        } catch (Exception e) {
            log.warn("[FeedbackProcessingEventPublisher] publish 실패 (event={})", event, e);
            return false;
        }
    }
}
