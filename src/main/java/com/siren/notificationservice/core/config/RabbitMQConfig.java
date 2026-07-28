package com.siren.notificationservice.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
     * Account API 등 외부 서비스가 발행하는 JSON 이벤트를 로컬 DTO로 역직렬화한다.
     * 기본값(__TypeId__ 헤더 기반)으로 두면 발행 측 클래스의 FQCN을 그대로 찾으려 해서
     * 우리 클래스패스에 없는 패키지라 역직렬화가 실패한다 — 리스너 메서드 파라미터 타입으로
     * 추론하도록(INFERRED) 바꿔서 발행 측 클래스명과 무관하게 동작하게 한다.
     *
     * @return JSON - DTO 변환기 (RabbitTemplate과 리스너 컨테이너에 자동 적용됨)
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(TypePrecedence.INFERRED);
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
