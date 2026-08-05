package com.siren.notificationservice.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "telegramEventsExchangeName", "telegram.events");
        ReflectionTestUtils.setField(config, "telegramInboundQueueName", "telegram.inbound.queue");
        ReflectionTestUtils.setField(config, "telegramInboundRoutingKey", "telegram.inbound");
        ReflectionTestUtils.setField(config, "notificationInternalExchangeName", "notification.events");
        ReflectionTestUtils.setField(config, "feedbackProcessingQueueName", "notification.feedback-processing.queue");
        ReflectionTestUtils.setField(config, "feedbackProcessingRoutingKey", "notification.feedback-processing");
    }

    @Test
    void telegramEventsExchangeHasCorrectName() {
        DirectExchange exchange = config.telegramEventsExchange();
        assertThat(exchange.getName()).isEqualTo("telegram.events");
    }

    @Test
    void notificationInternalExchangeHasCorrectName() {
        TopicExchange exchange = config.notificationInternalExchange();
        assertThat(exchange.getName()).isEqualTo("notification.events");
    }

    @Test
    void telegramInboundQueueIsDurable() {
        Queue queue = config.telegramInboundQueue();
        assertThat(queue.getName()).isEqualTo("telegram.inbound.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void feedbackProcessingQueueIsDurable() {
        Queue queue = config.feedbackProcessingQueue();
        assertThat(queue.getName()).isEqualTo("notification.feedback-processing.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void telegramInboundBindingConnectsQueueToExchange() {
        Queue queue = config.telegramInboundQueue();
        DirectExchange exchange = config.telegramEventsExchange();

        Binding binding = config.telegramInboundBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("telegram.events");
        assertThat(binding.getDestination()).isEqualTo("telegram.inbound.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("telegram.inbound");
    }

    @Test
    void feedbackProcessingBindingConnectsQueueToExchange() {
        Queue queue = config.feedbackProcessingQueue();
        TopicExchange exchange = config.notificationInternalExchange();

        Binding binding = config.feedbackProcessingBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("notification.events");
        assertThat(binding.getDestination()).isEqualTo("notification.feedback-processing.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("notification.feedback-processing");
    }

    @Test
    void jsonMessageConverterIsCreated() {
        assertThat(config.jsonMessageConverter()).isNotNull();
    }
}
