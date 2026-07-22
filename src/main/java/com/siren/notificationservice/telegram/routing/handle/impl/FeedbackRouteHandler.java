package com.siren.notificationservice.telegram.routing.handle.impl;

import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handle.IntentRouteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeedbackRouteHandler implements IntentRouteHandler {
    @Override
    public IntentType supports() {
        return IntentType.FEEDBACK;
    }

    @Override
    public void handle(TelegramInboundEvent event,Long userId) {
        //TODO: Issue: 63에서 수행
        log.info("[FeedbackRouteHandler] 실행");
    }
}
