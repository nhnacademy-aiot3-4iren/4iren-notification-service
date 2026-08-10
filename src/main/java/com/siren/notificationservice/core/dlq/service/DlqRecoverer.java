package com.siren.notificationservice.core.dlq.service;

import com.siren.notificationservice.core.dlq.DlqFailureDocument;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class DlqRecoverer extends RepublishMessageRecoverer{
    private final DlqFailureLogService dlqFailureLogService;
    private final MeterRegistry meterRegistry;

    public DlqRecoverer(RabbitTemplate rabbitTemplate,
                        @Value("${rabbitmq.exchage.dlx}")String dlxExchange,
                        @Value("${rabbitmq.routing-key.dlq}")String dlqRoutingKey,
                        MeterRegistry meterRegistry,
                        DlqFailureLogService dlqFailureLogService) {
        super(rabbitTemplate,dlxExchange,dlqRoutingKey);
        this.meterRegistry = meterRegistry;
        this.dlqFailureLogService = dlqFailureLogService;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        String correlationId = UUID.randomUUID().toString();
        String queue = message.getMessageProperties().getConsumerQueue();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        Throwable root = (cause.getCause() != null )? cause.getCause() : cause;

        dlqFailureLogService.save(DlqFailureDocument.builder()
                .id(correlationId)
                .queue(queue)
                .reason(root.getClass().getSimpleName())
                .exceptionMessage(root.getMessage())
                .stackTrace(stackTraceOf(root))
                .payload(payload)
                .occurredAt(Instant.now())
                .build());

        meterRegistry.counter("notification.dlq.messages",
                "queue", queue ==null ? "unknown" : queue,
                "reason", root.getClass().getSimpleName()).increment();

        super.recover(message, cause); // DLQ republish
    }

    private String stackTraceOf(Throwable root) {
        StringWriter sw = new StringWriter();
        root.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
