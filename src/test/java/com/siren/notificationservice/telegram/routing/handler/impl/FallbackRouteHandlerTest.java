package com.siren.notificationservice.telegram.routing.handler.impl;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FallbackRouteHandlerTest {

    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final FallbackRouteHandler fallbackRouteHandler = new FallbackRouteHandler(telegramMessageService);

    private TelegramInboundEvent textEvent() {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText("ㅋㅋㅋ");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    @Test
    void supportsReturnsFallback() {
        assertThat(fallbackRouteHandler.supports()).isEqualTo(IntentType.FALLBACK);
    }

    @Test
    void handleSendsFallbackMessage() {
        TelegramInboundEvent event = textEvent();

        fallbackRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendFallbackMessage(event.chatId(), BotType.USER_BOT);
    }
}
