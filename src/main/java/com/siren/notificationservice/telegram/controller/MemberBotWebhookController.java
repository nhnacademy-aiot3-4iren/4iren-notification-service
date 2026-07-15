package com.siren.notificationservice.telegram.controller;

import com.siren.notificationservice.core.entity.BotType;
import com.siren.notificationservice.telegram.dto.TelegramInboundEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberBotWebhookController {
    private final RabbitTemplate rabbitTemplate;
    @Value("${rabbitmq.exchange.telegram-events}")
    private String exchangeName;
    @Value("${rabbitmq.routing-key.telegram-inbound}")
    private String routingKey;

    @PostMapping("/webhook/member")
    public ResponseEntity<Void> webhookMember(@RequestBody Update update) {
        //토큰 검증 및 chat_id db 저장은 비동기로처리
        rabbitTemplate.convertAndSend(exchangeName, routingKey, new TelegramInboundEvent(BotType.USER_BOT, update));
        // 바로 200응답값 주기
        return ResponseEntity.ok().build();
    }
}
