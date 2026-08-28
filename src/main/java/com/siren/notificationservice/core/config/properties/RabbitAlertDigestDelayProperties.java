package com.siren.notificationservice.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabbitmq.alert.digest-delay")
public class RabbitAlertDigestDelayProperties {
    private String exchange;
    private String queue;
    private String routingKey;
    private long ttlMs;
    private String dlxExchange;
    private String flushQueue;
    private String flushRoutingKey;
}
