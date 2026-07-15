package com.siren.notificationservice.telegram.controller.webhook;

import com.siren.notificationservice.core.entity.BotType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "텔레그램 웹훅", description = "Telegram 서버가 직접 호출하는 웹훅 수신 엔드포인트 (사람이 API로 호출하는 용도 아님)")
public class AdminBotWebhookController {
    private final RabbitTemplate rabbitTemplate;
    @Value("${rabbitmq.exchange.telegram-events}")
    private String exchangeName;
    @Value("${rabbitmq.routing-key.telegram-inbound}")
    private String routingKey;

    @Operation(
            summary = "Admin 봇 웹훅 수신",
            description = "Telegram 서버가 Admin 봇으로 온 업데이트를 전달하는 콜백. "
                    + "수신 즉시 RabbitMQ(telegram-inbound)에 발행만 하고 200을 반환하며, "
                    + "실제 처리(TelegramInboundListener)는 비동기로 이뤄진다. "
                    + "Telegram 서버 전용 엔드포인트라 Swagger 'Try it out'으로 호출해도 의미 있는 응답이 오지 않는다."
    )
    @ApiResponse(responseCode = "200", description = "큐 발행 완료 (실제 비즈니스 처리 결과 아님)")
    @PostMapping("/webhook/admin")
    public ResponseEntity<Void> webhookAdmin(@RequestBody Update update) {
        //토큰 검증 및 chat_id db 저장은 비동기로처리
        //나중에 LLM도 들어오면 오래걸릴 수 있으니.. 텔레그램은 처리가 오래걸리면 실패로 간주하고 같은 업데이트를 다시 보내게됨 그럼
        // /start 토큰이 두번 소비되거나, DB row가 두 번 생성될 위험이 생김
        // 동시요청 보안 -> 동기처리하면 톰켓 스레드 풀이 금방 꽉차게 공
        rabbitTemplate.convertAndSend(exchangeName, routingKey, new TelegramInboundEvent(BotType.ADMIN_BOT, update));
        // rabbitMq로 보내고 바로 200리턴해주기
        return ResponseEntity.ok().build();
    }

}
