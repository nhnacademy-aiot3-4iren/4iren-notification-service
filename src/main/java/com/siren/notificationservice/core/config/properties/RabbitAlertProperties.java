package com.siren.notificationservice.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabbitmq.alert")
public class RabbitAlertProperties {
    private String exchange;
    private Urgent urgent;
    private Digest digest;

    @Getter
    @Setter
    public static class Urgent {
        private String queue;
        private String routingKey;
    }
    @Getter
    @Setter
    public static class Digest{
        private String queue;
        private String routingKey;
    }
}
