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

    @Value("${rabbitmq.exchange.account-events}")
    private String accountEventsExchangeName;

    @Value("${rabbitmq.queue.user-sync}")
    private String userSyncQueueName;

    @Value("${rabbitmq.routing-key.user-synced}")
    private String userSyncedRoutingKey;

    @Value("${rabbitmq.queue.subscribe-sync}")
    private String subscribeSyncQueueName;

    @Value("${rabbitmq.routing-key.subscribe-sync}")
    private String subscribeSyncRoutingKey;

    @Value("${rabbitmq.exchange.telegram-events}")
    private String telegramEventsExchangeName;

    @Value("${rabbitmq.queue.telegram-inbound}")
    private String telegramInboundQueueName;

    @Value("${rabbitmq.routing-key.telegram-inbound}")
    private String telegramInboundRoutingKey;

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
     * Account API가 유저 관련 이벤트를 발행하는 토픽 익스체인지.
     *
     * @return account-events 익스체인지
     */
    @Bean
    public TopicExchange accountEventsExchange() {
        return new TopicExchange(accountEventsExchangeName);
    }

    /**
     * 유저 동기화(UserSyncEvent) 전용 큐.
     *
     * @return durable 큐
     */
    @Bean
    public Queue userSyncQueue() {
        return new Queue(userSyncQueueName, true);
    }

    /**
     * userSyncQueue를 accountEventsExchange의 유저 동기화 라우팅 키에 바인딩한다.
     *
     * @param userSyncQueue         유저 동기화 큐
     * @param accountEventsExchange 계정 이벤트 익스체인지
     * @return 큐-익스체인지 바인딩
     */
    @Bean
    public Binding userSyncBinding(Queue userSyncQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(userSyncQueue).to(accountEventsExchange).with(userSyncedRoutingKey);
    }

    /**
     * 유저 구독 상태 동기화 전용 큐
     * @return durable 큐
     */
    @Bean
    public Queue subscribeSyncQueue() {
        return new Queue(subscribeSyncQueueName, true);
    }

    /**
     * subscribeSyncQueue를 accountEventsExchange의 유저 동기화 라우팅 키에 바인딩한다.
     * @param subscribeSyncQueue 유저 구독 동기화 큐
     * @param accountEventsExchange 계정 이벤트 익스체인지
     * @return 큐-익스체인지 바인딩
     */
    @Bean
    public Binding subscribeSyncBinding(Queue subscribeSyncQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(subscribeSyncQueue).to(accountEventsExchange).with(subscribeSyncRoutingKey);
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
     * Telegram inbound queue
     * @return durable 큐
     */
    @Bean
    public Queue telegramInboundQueue() {
        return new Queue(telegramInboundQueueName, true);
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
}
