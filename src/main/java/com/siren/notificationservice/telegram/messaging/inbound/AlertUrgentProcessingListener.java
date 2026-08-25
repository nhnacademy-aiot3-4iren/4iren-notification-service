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
public class AlertUrgentProcessingListener {

    private final AlertDispatchService alertDispatchService;

    /**
     * alert.urgent.* 라우팅 키로 들어오는 긴급 알림(COMFORT_LIMIT_EXCEEDED/SENSOR_ANOMALY)을 처리한다.
     * 예외를 삼키지 않고 그대로 던져서 재시도→DLQ로 보낸다.
     */
    @RabbitListener(queues = "#{@alertUrgentQueue.name}", containerFactory = "urgentContainerFactory")
    public void handle(AlertEvent event) {

        alertDispatchService.dispatchUrgent(event);
    }
}
