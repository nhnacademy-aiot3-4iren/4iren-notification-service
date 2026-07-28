package com.siren.notificationservice.telegram.callback.handler;

import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CallbackRouteDispatcher {
    private final Map<CallbackActionType, CallbackRouteHandler> handlers;

    public CallbackRouteDispatcher(List<CallbackRouteHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CallbackRouteHandler::supports, Function.identity()));
    }

    public void dispatch(CallbackActionType actionType, TelegramInboundEvent event, Long userId) {
        CallbackRouteHandler handler = handlers.get(actionType);
        if(handler == null) {
            throw new IllegalStateException("등록된 CallbackRouteHandler가 없습니다: "+ actionType);
        }
        handler.handle(event, userId);
    }
}
