package com.siren.notificationservice.telegram.routing.handle.impl;

import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handle.IntentRouteHandler;
import org.springframework.stereotype.Component;

@Component
public class FallbackRouteHandler implements IntentRouteHandler {
    @Override
    public IntentType supports() {
        return IntentType.FALLBACK;
    }

    @Override
    public void handle(TelegramInboundEvent event) {
        //TODO: Issue 63에서 수행
    }
}
