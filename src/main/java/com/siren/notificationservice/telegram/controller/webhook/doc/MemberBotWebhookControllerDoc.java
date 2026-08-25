package com.siren.notificationservice.telegram.controller.webhook.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.telegram.telegrambots.meta.api.objects.Update;

@Tag(name = "텔레그램 웹훅", description = "Telegram 서버가 직접 호출하는 웹훅 수신 엔드포인트 (사람이 API로 호출하는 용도 아님)")
public interface MemberBotWebhookControllerDoc {

    @Operation(
            summary = "Member 봇 웹훅 수신",
            description = "Telegram 서버가 Member 봇으로 온 업데이트를 전달하는 콜백. "
                    + "수신 즉시 RabbitMQ(telegram-inbound)에 발행만 하고 200을 반환하며, "
                    + "실제 처리(TelegramInboundListener)는 비동기로 이뤄진다. "
                    + "Telegram 서버 전용 엔드포인트라 Swagger 'Try it out'으로 호출해도 의미 있는 응답이 오지 않는다."
    )
    @ApiResponse(responseCode = "200", description = "큐 발행 완료 (실제 비즈니스 처리 결과 아님)")
    @PostMapping("/webhook/member")
    public ResponseEntity<Void> webhookMember(@RequestBody Update update);
}
