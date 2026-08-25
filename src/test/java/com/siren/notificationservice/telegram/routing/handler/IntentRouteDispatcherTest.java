package com.siren.notificationservice.telegram.routing.handler;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntentRouteDispatcherTest {

    private TelegramInboundEvent textEvent() {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText("너무 더워요");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    @Test
    void dispatchCallsMatchingHandler() {
        IntentRouteHandler feedbackHandler = mock(IntentRouteHandler.class);
        when(feedbackHandler.supports()).thenReturn(IntentType.FEEDBACK);
        IntentRouteDispatcher dispatcher = new IntentRouteDispatcher(List.of(feedbackHandler));
        TelegramInboundEvent event = textEvent();

        dispatcher.dispatch(IntentType.FEEDBACK, event, 1L);

        verify(feedbackHandler).handle(event, 1L);
    }

    @Test
    void dispatchThrowsWhenNoHandlerRegisteredForType() {
        IntentRouteDispatcher dispatcher = new IntentRouteDispatcher(List.of());
        TelegramInboundEvent event = textEvent();

        assertThatThrownBy(() -> dispatcher.dispatch(IntentType.FALLBACK, event, 1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
