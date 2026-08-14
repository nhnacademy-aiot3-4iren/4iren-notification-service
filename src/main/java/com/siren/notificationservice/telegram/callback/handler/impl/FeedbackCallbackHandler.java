package com.siren.notificationservice.telegram.callback.handler.impl;

import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.service.cache.FeedbackExtractionCacheService;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.callback.handler.CallbackRouteHandler;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.handler.impl.FeedbackRouteHandler;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FeedbackCallbackHandler implements CallbackRouteHandler {
    private final FeedbackExtractionCacheService feedbackExtractionCacheService;
    private final FeedbackRouteHandler feedbackRouteHandler;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackActionType supports() {
        return CallbackActionType.FEEDBACK_ROOM_SELECT;
    }

    @Override
    public void handle(TelegramInboundEvent event, Long userId) {
        Optional<FeedbackExtractionCache> cache = feedbackExtractionCacheService.find(userId);
        if(cache.isEmpty()) {
            telegramMessageService.sendFeedbackAlreadyHandledMessage(event.chatId(), event.botType());
            return;
        }

        Integer messageId = event.update().getCallbackQuery().getMessage().getMessageId();
        telegramMessageService.removeInlineKeyboard(event.chatId(), messageId, event.botType());
        feedbackRouteHandler.handleUserReply(event, userId, cache.get());
    }
}
