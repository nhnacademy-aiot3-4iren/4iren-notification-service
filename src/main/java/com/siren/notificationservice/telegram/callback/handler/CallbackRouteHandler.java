package com.siren.notificationservice.telegram.callback.handler;

import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;

public interface CallbackRouteHandler {
    CallbackActionType supports();

    void handle(TelegramInboundEvent event, Long userId);
}
