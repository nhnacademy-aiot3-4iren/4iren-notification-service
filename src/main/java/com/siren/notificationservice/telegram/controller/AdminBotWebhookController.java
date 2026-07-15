package com.siren.notificationservice.telegram.controller;

import com.siren.notificationservice.core.entity.BotType;
import com.siren.notificationservice.telegram.dto.TelegramInboundEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequiredArgsConstructor
public class AdminBotWebhookController {
    private final RabbitTemplate rabbitTemplate;
    @Value("${rabbitmq.exchange.telegram-events}")
    private String exchangeName;
    @Value("${rabbitmq.routing-key.telegram-inbound}")
    private String routingKey;

    @PostMapping("/webhook/admin")
    public ResponseEntity<Void> webhookAdmin(@RequestBody Update update) {
        //토큰 검증 및 chat_id db 저장은 비동기로처리
        //나중에 LLM도 들어오면 오래걸릴 수 있으니.. 텔레그램은 처리가 오래걸리면 실패로 간주하고 같은 업데이트를 다시 보내게됨 그럼
        // /start 토큰이 두번 소비되거나, DB row가 두 번 생성될 위험이 생김
        // 동시요청 보안 -> 동기처리하면 톰켓 스레드 풀이 금방 꽉차게 됨
        rabbitTemplate.convertAndSend(exchangeName, routingKey, new TelegramInboundEvent(BotType.ADMIN_BOT, update));
        // rabbitMq로 보내고 바로 200리턴해주기
        return ResponseEntity.ok().build();
    }

}
