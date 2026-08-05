package com.siren.notificationservice.telegram.callback.handler.impl;

import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.service.cache.FeedbackExtractionCacheService;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.handler.impl.FeedbackRouteHandler;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackCallbackHandlerTest {

    private final FeedbackExtractionCacheService feedbackExtractionCacheService = mock(FeedbackExtractionCacheService.class);
    private final FeedbackRouteHandler feedbackRouteHandler = mock(FeedbackRouteHandler.class);
    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final FeedbackCallbackHandler feedbackCallbackHandler =
            new FeedbackCallbackHandler(feedbackExtractionCacheService, feedbackRouteHandler, telegramMessageService);

    private TelegramInboundEvent textEvent() {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText("301호");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    @Test
    void supportsReturnsFeedbackRoomSelect() {
        assertThat(feedbackCallbackHandler.supports()).isEqualTo(CallbackActionType.FEEDBACK_ROOM_SELECT);
    }

    @Test
    void handleDelegatesToFeedbackRouteHandlerWhenCacheExists() {
        FeedbackExtractionCache cache = new FeedbackExtractionCache("더워요", List.of(), null);
        when(feedbackExtractionCacheService.find(1L)).thenReturn(Optional.of(cache));
        TelegramInboundEvent event = textEvent();

        feedbackCallbackHandler.handle(event, 1L);

        verify(feedbackRouteHandler).handleUserReply(event, 1L, cache);
        verify(telegramMessageService, never()).sendFeedbackProcessingFailedMessage(any(), any());
    }

    @Test
    void handleSendsFailedMessageWhenCacheExpired() {
        when(feedbackExtractionCacheService.find(1L)).thenReturn(Optional.empty());
        TelegramInboundEvent event = textEvent();

        feedbackCallbackHandler.handle(event, 1L);

        verify(telegramMessageService).sendFeedbackProcessingFailedMessage(event.chatId(), BotType.USER_BOT);
        verify(feedbackRouteHandler, never()).handleUserReply(any(), any(), any());
    }
}
