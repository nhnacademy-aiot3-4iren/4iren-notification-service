package com.siren.notificationservice.telegram.routing.handle;

import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;

public interface IntentRouteHandler {

    /**
     * 이 핸들러가 처리하는 의도 타입을 반환한다.
     *
     * @return 이 핸들러가 담당하는 IntentType
     */
    IntentType supports();

    /**
     * 분류된 의도에 맞는 실제 처리를 수행한다.
     *
     * @param event 원본 텔레그램 인바운드 이벤트
     */
    void handle(TelegramInboundEvent event);
}
