package com.siren.notificationservice.telegram.routing.handle;

import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 의도 분류된 핸들러들의 디스패처
 */
@Component
public class IntentRouteDispatcher{

    private final Map<IntentType, IntentRouteHandler> handlers;

    /**
     * Spring이 등록된 모든 IntentRouteHandler 빈을 리스트로 주입해주면, supports()가 반환하는
     * IntentType을 key로 하는 맵으로 변환해둔다. 새 의도가 추가되면 이 클래스는 안 건드리고
     * IntentRouteHandler 구현체(@Component)만 추가하면 된다.
     *
     * @param handlerList Spring이 수집한 IntentRouteHandler 빈 목록
     */
    public IntentRouteDispatcher(List<IntentRouteHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(IntentRouteHandler::supports, Function.identity()));
    }

    /**
     * 분류된 의도에 해당하는 핸들러를 찾아 바로 실행한다.
     * 해당 IntentType을 처리하는 핸들러가 등록돼 있지 않으면(설정 누락) 예외를 던진다 —
     * 호출부에서 null 체크를 반복하지 않도록 조회와 실행, 오류 처리를 여기서 한 번에 끝낸다.
     *
     * @param intentType 분류된 의도
     * @param event      원본 텔레그램 인바운드 이벤트
     */
    public void dispatch(IntentType intentType, TelegramInboundEvent event, Long userId) {
        IntentRouteHandler handler = handlers.get(intentType);
        if (handler == null) {
            throw new IllegalStateException("등록된 IntentRouteHandler가 없습니다: " + intentType);
        }
        handler.handle(event, userId);
    }
}
