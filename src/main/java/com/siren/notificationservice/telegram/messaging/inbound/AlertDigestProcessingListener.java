package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.service.AlertDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertDigestProcessingListener {

    private final AlertDispatchService alertDispatchService;

    /**
     * alert.digest.* 라우팅 키로 들어오는 비긴급 알림(VENTILATION_RECOMMEND)을 처리한다.
     * DLQ가 없어서 우선 try-catch로 무한 재큐잉 방지함.
     */
    @RabbitListener(queues = "#{@alertDigestQueue.name}")
    public void handle(AlertEvent event) {
        try {
            alertDispatchService.dispatchDigest(event);
        } catch (Exception e) {
            log.error("[AlertDigestProcessingListener] 비긴급 알림 처리 실패, 데이터 유실 가능 (event={})", event, e);
        }
    }
}
