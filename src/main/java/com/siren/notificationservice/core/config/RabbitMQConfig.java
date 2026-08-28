package com.siren.notificationservice.core.config;

import com.siren.notificationservice.core.config.properties.RabbitAccountProperties;
import com.siren.notificationservice.core.config.properties.RabbitAlertDigestDelayProperties;
import com.siren.notificationservice.core.config.properties.RabbitAlertProperties;
import com.siren.notificationservice.core.config.properties.RabbitFeedbackProcessingProperties;
import com.siren.notificationservice.core.config.properties.RabbitNotificationDlqProperties;
import com.siren.notificationservice.core.config.properties.RabbitTelegramInboundProperties;
import com.siren.notificationservice.core.dlq.service.DlqRecoverer;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;

import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    private final RabbitTelegramInboundProperties inbound;
    private final RabbitFeedbackProcessingProperties feedback;
    private final RabbitAlertProperties alert;
    private final RabbitAlertDigestDelayProperties alertDigestDelay;
    private final RabbitAccountProperties account;
    private final RabbitNotificationDlqProperties dlqProperties;

    /**
     * @return JSON - DTO 변환기 (RabbitTemplate과 리스너 컨테이너에 자동 적용됨)
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    // Telegram Inbound
    /**
     * Telegram inbound exchange
     */
    @Bean
    public DirectExchange telegramEventsExchange() {
        return new DirectExchange(inbound.getExchange());
    }
    /**
     * Telegram inbound queue
     */
    @Bean
    public Queue telegramInboundQueue() {
        return new Queue(inbound.getQueue(), true);
    }
    /**
     * telegramInboundQueue를 telegramEventsExchange의 telegram inbound 라우팅 키에 바인딩한다.
     */
    @Bean
    public Binding telegramInboundBinding(Queue telegramInboundQueue, DirectExchange telegramEventsExchange) {
        return BindingBuilder.bind(telegramInboundQueue).to(telegramEventsExchange).with(inbound.getRoutingKey());
    }


    // Telegram feedback
    /**
     * feedback 수집 및 저장을 위한 exchange (telegram-events와 별개 — 텔레그램 인바운드가 아니라
     * 확정된 도메인 이벤트를 다루는 내부 파이프라인이라 의미상 분리)
     */
    @Bean
    public TopicExchange notificationInternalExchange() {
        return new TopicExchange(feedback.getExchange());
    }
    /**
     * 피드백 확정 후 무거운 처리(환경 스냅샷 조회 + DB 저장)를 비동기로 넘기는 내부 큐.
     */
    @Bean
    public Queue feedbackProcessingQueue() {
        return new Queue(feedback.getQueue(), true);
    }
    /**
     * feedbackProcessingQueue를 notificationInternalExchange()의 feedbackProcessingRoutingKey에 바인딩한다.
     */
    @Bean
    public Binding feedbackProcessingBinding(Queue feedbackProcessingQueue, TopicExchange notificationInternalExchange) {
        return BindingBuilder.bind(feedbackProcessingQueue).to(notificationInternalExchange).with(feedback.getRoutingKey());
    }

    // Telegram Alert
    /**
     * Processing/RuleEngine이 발행하는 AlertEvent용 익스체인지
     */
    @Bean
    public TopicExchange alertEventsExchange() {
        return new TopicExchange(alert.getExchange());
    }
    /**
     * 긴급 알림(COMFORT_LIMIT_EXCEEDED/SENSOR_ANOMALY) 전용 큐.
     */
    @Bean
    public Queue alertUrgentQueue() {
        return new Queue(alert.getUrgent().getQueue(), true);
    }
    /**
     * 비긴급 알림(VENTILATION_RECOMMEND) 전용 큐.
     */
    @Bean
    public Queue alertDigestQueue() {
        return new Queue(alert.getDigest().getQueue(), true);
    }
    /**
     * alertUrgentQueue를 라우팅 키(와일드카드)에 바인딩
     */
    @Bean
    public Binding alertUrgentBinding(Queue alertUrgentQueue, TopicExchange alertEventsExchange) {
        return BindingBuilder.bind(alertUrgentQueue).to(alertEventsExchange).with(alert.getUrgent().getRoutingKey());
    }
    /**
     * alertDigestQueue를 라우팅 키(와일드카드)에 바인딩
     */
    @Bean
    public Binding alertDigestBinding(Queue alertDigestQueue, TopicExchange alertEventsExchange) {
        return BindingBuilder.bind(alertDigestQueue).to(alertEventsExchange).with(alert.getDigest().getRoutingKey());
    }

    //이하 디바운스 버퍼 (알림발송로직의 비긴급일때 인스턴스 갯수에 따른 중복로직을 방어하기위해..)
    /** 발행처 -> 대기 큐로 넣는 익스체인지 */
    @Bean
    public DirectExchange alertDigestDelayExchange() {
        return new DirectExchange(alertDigestDelay.getExchange());
    }

    /** dead-letter 익스체인지 (TTL 만료된 메시지가 여기로 이동) */
    @Bean
    public DirectExchange alertDigestDlxExchange() {
        return new DirectExchange(alertDigestDelay.getDlxExchange());
    }

    /**
     * 대기 큐 - 컨슈머 없음. 큐 레벨 TTL(x-message-ttl)로 N분 뒤 만료되면 (지금은 3분으로 잡아놈 프로퍼티 확인)
     * x-dead-letter-exchange(DLX)로 이동한다. 이 만료 -> 이동이 타이머 역할.
     */
    @Bean
    public Queue alertDigestDelayQueue() {
        return QueueBuilder.durable(alertDigestDelay.getQueue())
                .ttl((int) alertDigestDelay.getTtlMs())
                .deadLetterExchange(alertDigestDelay.getDlxExchange()) // 대기큐랑 dlx 익스체인지랑 연결
                .deadLetterRoutingKey(alertDigestDelay.getFlushRoutingKey()) // 프로듀서 처럼 익스체인지랑 라우팅 키로만 연결하면 큐 찾아감
                .build();
    }

    /** flush 큐 - 컨슈머 있음(flush 리스너) */
    @Bean
    public Queue alertDigestFlushQueue() {
        return new Queue(alertDigestDelay.getFlushQueue(), true);
    }

    /** 발행처 -> 대기 큐 바인딩 */
    @Bean
    public Binding alertDigestDelayBinding(Queue alertDigestDelayQueue, DirectExchange alertDigestDelayExchange) {
        return BindingBuilder.bind(alertDigestDelayQueue).to(alertDigestDelayExchange).with(alertDigestDelay.getRoutingKey());
    }

    /** DLX -> flush 큐 바인딩 (만료된 메시지가 여기로 도착) */
    @Bean
    public Binding alertDigestFlushBinding(Queue alertDigestFlushQueue, DirectExchange alertDigestDlxExchange) {
        return BindingBuilder.bind(alertDigestFlushQueue).to(alertDigestDlxExchange).with(alertDigestDelay.getFlushRoutingKey());
    }

    // Account
    @Bean
    public DirectExchange accountRoleExchange() { return new DirectExchange(account.getExchange());}

    @Bean
    public Queue accountRoleQueue() {return new Queue(account.getQueue(), true);}

    @Bean
    public Binding accountRoleBinding(Queue accountRoleQueue, DirectExchange accountRoleExchange) {
        return BindingBuilder.bind(accountRoleQueue).to(accountRoleExchange).with(account.getRoutingKey());
    }


    // DLQ
    @Bean
    public DirectExchange dlxExchange() {return new DirectExchange(dlqProperties.getExchange());}

    @Bean
    public Queue dlq() {return new Queue(dlqProperties.getQueue(), true);}

    @Bean
    public Binding dlqBinding(Queue dlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlq).to(dlxExchange).with(dlqProperties.getRoutingKey());
    }

    // urgent 전용 팩토리 (나머지는 프로퍼티를 따라가게)
    @Bean
    public SimpleRabbitListenerContainerFactory urgentContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory, DlqRecoverer dlqRecoverer) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        configurer.configure(factory, connectionFactory);

        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(2).backOffOptions(500, 2.0, 2000).recoverer(dlqRecoverer).build());
        return factory;
    }
}
