package com.siren.notificationservice.telegram.controller.webhook;

import com.siren.notificationservice.core.config.properties.RabbitTelegramInboundProperties;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.controller.webhook.doc.MemberBotWebhookControllerDoc;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberBotWebhookController implements MemberBotWebhookControllerDoc {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTelegramInboundProperties inbound;

    @PostMapping("/webhook/member")
    public ResponseEntity<Void> webhookMember(@RequestBody Update update) {
        //토큰 검증 및 chat_id db 저장은 비동기로처리
        rabbitTemplate.convertAndSend(inbound.getExchange(), inbound.getRoutingKey(), new TelegramInboundEvent(BotType.USER_BOT, update));
        // 바로 200응답값 주기
        return ResponseEntity.ok().build();
    }
}
