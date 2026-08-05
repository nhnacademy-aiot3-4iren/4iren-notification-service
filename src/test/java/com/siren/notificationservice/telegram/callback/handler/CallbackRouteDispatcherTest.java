package com.siren.notificationservice.telegram.callback.handler;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackRouteDispatcherTest {

    private TelegramInboundEvent textEvent() {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText("안녕");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    @Test
    void dispatchCallsMatchingHandler() {
        CallbackRouteHandler feedbackHandler = mock(CallbackRouteHandler.class);
        when(feedbackHandler.supports()).thenReturn(CallbackActionType.FEEDBACK_ROOM_SELECT);
        CallbackRouteDispatcher dispatcher = new CallbackRouteDispatcher(List.of(feedbackHandler));
        TelegramInboundEvent event = textEvent();

        dispatcher.dispatch(CallbackActionType.FEEDBACK_ROOM_SELECT, event, 1L);

        verify(feedbackHandler).handle(event, 1L);
    }

    @Test
    void dispatchThrowsWhenNoHandlerRegisteredForType() {
        CallbackRouteDispatcher dispatcher = new CallbackRouteDispatcher(List.of());
        TelegramInboundEvent event = textEvent();

        assertThatThrownBy(() -> dispatcher.dispatch(CallbackActionType.QUESTION_CONTINUE, event, 1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
