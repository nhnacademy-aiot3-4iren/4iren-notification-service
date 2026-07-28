package com.siren.notificationservice.telegram.callback.handler.impl;

import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.callback.handler.CallbackRouteHandler;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.handler.impl.QuestionRouteHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionCallbackHandler implements CallbackRouteHandler {
    private final QuestionRouteHandler questionRouteHandler;
    @Override
    public CallbackActionType supports() {
        return CallbackActionType.QUESTION_CONTINUE;
    }

    @Override
    public void handle(TelegramInboundEvent event, Long userId) {
        questionRouteHandler.handle(event, userId);
    }
}
