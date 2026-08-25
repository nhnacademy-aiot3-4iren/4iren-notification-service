package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.service.alert.AlertDispatchService;
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
     * 예외를 삼키지 않고 그대로 던져서 재시도→DLQ로 보낸다.
     */
    @RabbitListener(queues = "#{@alertDigestQueue.name}")
    public void handle(AlertEvent event) {
        alertDispatchService.dispatchDigest(event);
    }
}
