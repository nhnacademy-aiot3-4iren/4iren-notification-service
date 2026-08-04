package com.siren.notificationservice.telegram.callback.handler.impl;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.handler.impl.QuestionRouteHandler;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class QuestionCallbackHandlerTest {

    private final QuestionRouteHandler questionRouteHandler = mock(QuestionRouteHandler.class);
    private final QuestionCallbackHandler questionCallbackHandler = new QuestionCallbackHandler(questionRouteHandler);

    private TelegramInboundEvent textEvent() {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText("몇 도야?");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    @Test
    void supportsReturnsQuestionContinue() {
        assertThat(questionCallbackHandler.supports()).isEqualTo(CallbackActionType.QUESTION_CONTINUE);
    }

    @Test
    void handleDelegatesToQuestionRouteHandler() {
        TelegramInboundEvent event = textEvent();

        questionCallbackHandler.handle(event, 1L);

        verify(questionRouteHandler).handle(event, 1L);
    }
}
