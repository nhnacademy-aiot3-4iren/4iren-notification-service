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
        ReflectionTestUtils.setField(config, "telegramInboundQueueName", "notification.telegram-inbound.queue");
        ReflectionTestUtils.setField(config, "telegramInboundRoutingKey", "telegram.inbound");
        ReflectionTestUtils.setField(config, "notificationInternalExchangeName", "notification.events");
        ReflectionTestUtils.setField(config, "feedbackProcessingQueueName", "notification.feedback-processing.queue");
        ReflectionTestUtils.setField(config, "feedbackProcessingRoutingKey", "notification.feedback-processing");

        ReflectionTestUtils.setField(config, "alertEventsExchangeName", "alert.events");
        ReflectionTestUtils.setField(config, "alertUrgentQueueName", "alert.urgent.queue");
        ReflectionTestUtils.setField(config, "alertUrgentRoutingKey", "alert.urgent.*");
        ReflectionTestUtils.setField(config, "alertDigestQueueName", "alert.digest.queue");
        ReflectionTestUtils.setField(config, "alertDigestRoutingKey", "alert.digest.*");

        ReflectionTestUtils.setField(config, "alertDigestDelayExchangeName", "alert.delay");
        ReflectionTestUtils.setField(config, "alertDigestDelayQueueName", "alert.delay.queue");
        ReflectionTestUtils.setField(config, "alertDigestDelayRoutingKey", "alert.delay");
        ReflectionTestUtils.setField(config, "alertDigestDlxExchangeName", "alert.delay.dlx");
        ReflectionTestUtils.setField(config, "alertDigestFlushQueueName", "alert.delay.flush.queue");
        ReflectionTestUtils.setField(config, "alertDigestFlushRoutingKey", "alert.delay.dlx-flush");
        ReflectionTestUtils.setField(config, "alertDigestTtlMs", 180000L);

        ReflectionTestUtils.setField(config, "dlxExchangeName", "notification.dlx");
        ReflectionTestUtils.setField(config, "dlqQueueName", "notification.dlq");
        ReflectionTestUtils.setField(config, "dlqRoutingKey", "notification.dlq");
    }

    // --- Telegram inbound ---

    @Test
    void telegramEventsExchangeHasCorrectName() {
        DirectExchange exchange = config.telegramEventsExchange();
        assertThat(exchange.getName()).isEqualTo("telegram.events");
    }

    @Test
    void telegramInboundQueueIsDurable() {
        Queue queue = config.telegramInboundQueue();
        assertThat(queue.getName()).isEqualTo("notification.telegram-inbound.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void telegramInboundBindingConnectsQueueToExchange() {
        Queue queue = config.telegramInboundQueue();
        DirectExchange exchange = config.telegramEventsExchange();

        Binding binding = config.telegramInboundBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("telegram.events");
        assertThat(binding.getDestination()).isEqualTo("notification.telegram-inbound.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("telegram.inbound");
    }

    // --- Feedback processing (internal) ---

    @Test
    void notificationInternalExchangeHasCorrectName() {
        TopicExchange exchange = config.notificationInternalExchange();
        assertThat(exchange.getName()).isEqualTo("notification.events");
    }

    @Test
    void feedbackProcessingQueueIsDurable() {
        Queue queue = config.feedbackProcessingQueue();
        assertThat(queue.getName()).isEqualTo("notification.feedback-processing.queue");
        assertThat(queue.isDurable()).isTrue();
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

    // --- Alert events ---

    @Test
    void alertEventsExchangeHasCorrectName() {
        TopicExchange exchange = config.alertEventsExchange();
        assertThat(exchange.getName()).isEqualTo("alert.events");
    }

    @Test
    void alertUrgentQueueIsDurable() {
        Queue queue = config.alertUrgentQueue();
        assertThat(queue.getName()).isEqualTo("alert.urgent.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void alertDigestQueueIsDurable() {
        Queue queue = config.alertDigestQueue();
        assertThat(queue.getName()).isEqualTo("alert.digest.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void alertUrgentBindingUsesWildcardRoutingKey() {
        Queue queue = config.alertUrgentQueue();
        TopicExchange exchange = config.alertEventsExchange();

        Binding binding = config.alertUrgentBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("alert.events");
        assertThat(binding.getDestination()).isEqualTo("alert.urgent.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("alert.urgent.*");
    }

    @Test
    void alertDigestBindingUsesWildcardRoutingKey() {
        Queue queue = config.alertDigestQueue();
        TopicExchange exchange = config.alertEventsExchange();

        Binding binding = config.alertDigestBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("alert.events");
        assertThat(binding.getDestination()).isEqualTo("alert.digest.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("alert.digest.*");
    }

    // --- Alert digest debounce buffer (delay queue + DLX + flush) ---

    @Test
    void alertDigestDelayExchangeHasCorrectName() {
        DirectExchange exchange = config.alertDigestDelayExchange();
        assertThat(exchange.getName()).isEqualTo("alert.delay");
    }

    @Test
    void alertDigestDlxExchangeHasCorrectName() {
        DirectExchange exchange = config.alertDigestDlxExchange();
        assertThat(exchange.getName()).isEqualTo("alert.delay.dlx");
    }

    @Test
    void alertDigestDelayQueueHasTtlAndDeadLetterArguments() {
        Queue queue = config.alertDigestDelayQueue();

        assertThat(queue.getName()).isEqualTo("alert.delay.queue");
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-message-ttl", 180000)
                .containsEntry("x-dead-letter-exchange", "alert.delay.dlx")
                .containsEntry("x-dead-letter-routing-key", "alert.delay.dlx-flush");
    }

    @Test
    void alertDigestFlushQueueIsDurable() {
        Queue queue = config.alertDigestFlushQueue();
        assertThat(queue.getName()).isEqualTo("alert.delay.flush.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void alertDigestDelayBindingConnectsDelayQueueToDelayExchange() {
        Queue queue = config.alertDigestDelayQueue();
        DirectExchange exchange = config.alertDigestDelayExchange();

        Binding binding = config.alertDigestDelayBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("alert.delay");
        assertThat(binding.getDestination()).isEqualTo("alert.delay.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("alert.delay");
    }

    @Test
    void alertDigestFlushBindingMatchesDelayQueueDeadLetterRoutingKey() {
        Queue queue = config.alertDigestFlushQueue();
        DirectExchange exchange = config.alertDigestDlxExchange();

        Binding binding = config.alertDigestFlushBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("alert.delay.dlx");
        assertThat(binding.getDestination()).isEqualTo("alert.delay.flush.queue");
        // 만료 메시지가 DLX에서 flush 큐로 라우팅되려면 delay 큐의 x-dead-letter-routing-key와 동일해야 한다
        assertThat(binding.getRoutingKey()).isEqualTo("alert.delay.dlx-flush");
    }

    // --- DLQ ---

    @Test
    void dlxExchangeHasCorrectName() {
        DirectExchange exchange = config.dlxExchange();
        assertThat(exchange.getName()).isEqualTo("notification.dlx");
    }

    @Test
    void dlqIsDurable() {
        Queue queue = config.dlq();
        assertThat(queue.getName()).isEqualTo("notification.dlq");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void dlqBindingConnectsDlqToDlxExchange() {
        Queue queue = config.dlq();
        DirectExchange exchange = config.dlxExchange();

        Binding binding = config.dlqBinding(queue, exchange);

        assertThat(binding.getExchange()).isEqualTo("notification.dlx");
        assertThat(binding.getDestination()).isEqualTo("notification.dlq");
        assertThat(binding.getRoutingKey()).isEqualTo("notification.dlq");
    }

    // --- Converter ---

    @Test
    void jsonMessageConverterIsCreated() {
        assertThat(config.jsonMessageConverter()).isNotNull();
    }
}
