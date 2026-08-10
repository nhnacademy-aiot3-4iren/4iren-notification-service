package com.siren.notificationservice.core.config;

import org.springframework.amqp.core.*;

import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.telegram-events}") private String telegramEventsExchangeName;
    @Value("${rabbitmq.queue.telegram-inbound}") private String telegramInboundQueueName;
    @Value("${rabbitmq.routing-key.telegram-inbound}") private String telegramInboundRoutingKey;
    @Value("${rabbitmq.exchange.notification-internal}") private String notificationInternalExchangeName;
    @Value("${rabbitmq.queue.feedback-processing}") private String feedbackProcessingQueueName;
    @Value("${rabbitmq.routing-key.feedback-processing}") private String feedbackProcessingRoutingKey;
    @Value("${rabbitmq.exchange.alert-events}") private String alertEventsExchangeName;
    @Value("${rabbitmq.queue.alert-urgent}") private String alertUrgentQueueName;
    @Value("${rabbitmq.routing-key.alert-urgent}") private String alertUrgentRoutingKey;
    @Value("${rabbitmq.queue.alert-digest}") private String alertDigestQueueName;
    @Value("${rabbitmq.routing-key.alert-digest}") private String alertDigestRoutingKey;

    @Value("${rabbitmq.exchange.alert-digest-delay}")   private String alertDigestDelayExchangeName;
    @Value("${rabbitmq.queue.alert-digest-delay}")      private String alertDigestDelayQueueName;
    @Value("${rabbitmq.routing-key.alert-digest-delay}") private String alertDigestDelayRoutingKey;
    @Value("${rabbitmq.exchange.alert-digest-dlx}")     private String alertDigestDlxExchangeName;
    @Value("${rabbitmq.queue.alert-digest-flush}")      private String alertDigestFlushQueueName;
    @Value("${rabbitmq.routing-key.alert-digest-flush}") private String alertDigestFlushRoutingKey;
    @Value("${rabbitmq.alert-digest.ttl-ms}")           private long alertDigestTtlMs;

    /**
     * @return JSON - DTO 변환기 (RabbitTemplate과 리스너 컨테이너에 자동 적용됨)
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    /**
     * Telegram inbound exchange
     *
     * @return telegram-events 익스체인지
     */
    @Bean
    public DirectExchange telegramEventsExchange() {
        return new DirectExchange(telegramEventsExchangeName);
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
    public Binding telegramInboundBinding(Queue telegramInboundQueue, DirectExchange telegramEventsExchange) {
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

    /**
     * Processing/RuleEngine이 발행하는 AlertEvent용 익스체인지
     * @return alert-events 익스체인지
     */
    @Bean
    public TopicExchange alertEventsExchange() {
        return new TopicExchange(alertEventsExchangeName);
    }

    /**
     * 긴급 알림(COMFORT_LIMIT_EXCEEDED/SENSOR_ANOMALY) 전용 큐.
     */
    @Bean
    public Queue alertUrgentQueue() {
        return new Queue(alertUrgentQueueName, true);
    }

    /**
     * 비긴급 알림(VENTILATION_RECOMMEND) 전용 큐.
     */
    @Bean
    public Queue alertDigestQueue() {
        return new Queue(alertDigestQueueName, true);
    }

    /**
     * alertUrgentQueue를 라우팅 키(와일드카드)에 바인딩
     */
    @Bean
    public Binding alertUrgentBinding(Queue alertUrgentQueue, TopicExchange alertEventsExchange) {
        return BindingBuilder.bind(alertUrgentQueue).to(alertEventsExchange).with(alertUrgentRoutingKey);
    }

    /**
     * alertDigestQueue를 라우팅 키(와일드카드)에 바인딩
     */
    @Bean
    public Binding alertDigestBinding(Queue alertDigestQueue, TopicExchange alertEventsExchange) {
        return BindingBuilder.bind(alertDigestQueue).to(alertEventsExchange).with(alertDigestRoutingKey);
    }

    //이하 디바운스 버퍼 (알림발송로직의 비긴급일때 인스턴스 갯수에 따른 중복로직을 방어하기위해..)
    /** 발행처 -> 대기 큐로 넣는 익스체인지 */
    @Bean
    public DirectExchange alertDigestDelayExchange() {
        return new DirectExchange(alertDigestDelayExchangeName);
    }

    /** dead-letter 익스체인지 (TTL 만료된 메시지가 여기로 이동) */
    @Bean
    public DirectExchange alertDigestDlxExchange() {
        return new DirectExchange(alertDigestDlxExchangeName);
    }

    /**
     * 대기 큐 - 컨슈머 없음. 큐 레벨 TTL(x-message-ttl)로 N분 뒤 만료되면 (지금은 3분으로 잡아놈 프로퍼티 확인)
     * x-dead-letter-exchange(DLX)로 이동한다. 이 만료 -> 이동이 타이머 역할.
     */
    @Bean
    public Queue alertDigestDelayQueue() {
        return QueueBuilder.durable(alertDigestDelayQueueName)
                .ttl((int) alertDigestTtlMs)
                .deadLetterExchange(alertDigestDlxExchangeName) // 대기큐랑 dlx 익스체인지랑 연결
                .deadLetterRoutingKey(alertDigestFlushRoutingKey) // 프로듀서 처럼 익스체인지랑 라우팅 키로만 연결하면 큐 찾아감
                .build();
    }

    /** flush 큐 - 컨슈머 있음(flush 리스너) */
    @Bean
    public Queue alertDigestFlushQueue() {
        return new Queue(alertDigestFlushQueueName, true);
    }

    /** 발행처 -> 대기 큐 바인딩 */
    @Bean
    public Binding alertDigestDelayBinding(Queue alertDigestDelayQueue, DirectExchange alertDigestDelayExchange) {
        return BindingBuilder.bind(alertDigestDelayQueue).to(alertDigestDelayExchange).with(alertDigestDelayRoutingKey);
    }

    /** DLX -> flush 큐 바인딩 (만료된 메시지가 여기로 도착) */
    @Bean
    public Binding alertDigestFlushBinding(Queue alertDigestFlushQueue, DirectExchange alertDigestDlxExchange) {
        return BindingBuilder.bind(alertDigestFlushQueue).to(alertDigestDlxExchange).with(alertDigestFlushRoutingKey);
    }

}
