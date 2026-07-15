package com.siren.notificationservice.telegram.messaging;

import com.siren.notificationservice.telegram.dto.TelegramInboundEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class TelegramInboundListener {

    @RabbitListener(queues = "#{@telegramInboundQueue.name}")
    public void handle (TelegramInboundEvent event){
        Update update = event.update();

        if (update.hasMessage() && update.getMessage().hasText()
                && update.getMessage().getText().startsWith("/start")) {
            // TODO #59: 토큰 검증 + chat_id 매핑
        } else if (update.hasMyChatMember()) {
            // TODO #61: 봇 차단 감지
        }
        // 그 외 자연어 메시지는 Phase 5 의도분류에서 처리
    }
}
