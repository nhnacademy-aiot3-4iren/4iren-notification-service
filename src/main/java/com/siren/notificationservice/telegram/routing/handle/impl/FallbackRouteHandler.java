package com.siren.notificationservice.telegram.routing.handle.impl;

import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handle.IntentRouteHandler;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FallbackRouteHandler implements IntentRouteHandler {
    private final TelegramMessageService telegramMessageService;
    @Override
    public IntentType supports() {
        return IntentType.FALLBACK;
    }

    @Override
    public void handle(TelegramInboundEvent event, Long userId) {
        telegramMessageService.sendFallbackMessage(event.chatId(), event.botType());
    }
}
