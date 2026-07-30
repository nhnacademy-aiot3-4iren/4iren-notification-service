package com.siren.notificationservice.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.telegram-events}")
    private String telegramEventsExchangeName;

    @Value("${rabbitmq.queue.telegram-inbound}")
    private String telegramInboundQueueName;

    @Value("${rabbitmq.routing-key.telegram-inbound}")
    private String telegramInboundRoutingKey;

    @Value("${rabbitmq.exchange.notification-internal}")
    private String notificationInternalExchangeName;

    @Value("${rabbitmq.queue.feedback-processing}")
    private String feedbackProcessingQueueName;

    @Value("${rabbitmq.routing-key.feedback-processing}")
    private String feedbackProcessingRoutingKey;

    /**
     * @return JSON - DTO 변환기 (RabbitTemplate과 리스너 컨테이너에 자동 적용됨)
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    /**
     * Telegram inbound exchange
     *
     * @return telegram-events 익스체인지
     */
    @Bean
    public TopicExchange telegramEventsExchange() {
        return new TopicExchange(telegramEventsExchangeName);
    }

    /**
     * feedback 수집 및 저장을 위한 exchange (telegram-events와 별개 — 텔레그램 인바운드가 아니라
     * 확정된 도메인 이벤트를 다루는 내부 파이프라인이라 의미상 분리)
     *
     * @return notification-internal 익스체인지
     */
    @Bean
    public TopicExchange notificationInternalExchange() {
        return new TopicExchange(notificationInternalExchangeName);
    }

    /**
     * Telegram inbound queue
     * @return durable 큐
     */
    @Bean
    public Queue telegramInboundQueue() {
        return new Queue(telegramInboundQueueName, true);
    }

    /**
     * 피드백 확정 후 무거운 처리(환경 스냅샷 조회 + DB 저장)를 비동기로 넘기는 내부 큐.
     */
    @Bean
    public Queue feedbackProcessingQueue() {
        return new Queue(feedbackProcessingQueueName, true);
    }

    /**
     * telegramInboundQueue를 telegramEventsExchange의 telegram inbound 라우팅 키에 바인딩한다.
     * @param telegramInboundQueue 텔레그램 인바운드 큐
     * @param telegramEventsExchange 텔레그램 인바운드 익스체인지
     * @return 큐-익스체인지 바인딩
     */
    @Bean
    public Binding telegramInboundBinding(Queue telegramInboundQueue, TopicExchange telegramEventsExchange) {
        return BindingBuilder.bind(telegramInboundQueue).to(telegramEventsExchange).with(telegramInboundRoutingKey);
    }

    /**
     * feedbackProcessingQueue를 notificationInternalExchange()의 feedbackProcessingRoutingKey에 바인딩한다.
     * @param feedbackProcessingQueue 피드백 처리 큐
     * @param notificationInternalExchange notification 내부 익스체인지
     * @return 큐-익스체인지 바인딩
     */
    @Bean
    public Binding feedbackProcessingBinding(Queue feedbackProcessingQueue, TopicExchange notificationInternalExchange) {
        return BindingBuilder.bind(feedbackProcessingQueue).to(notificationInternalExchange).with(feedbackProcessingRoutingKey);
    }

}
