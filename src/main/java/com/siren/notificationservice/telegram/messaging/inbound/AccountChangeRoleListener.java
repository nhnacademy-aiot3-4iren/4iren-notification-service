package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.dto.event.AccountRoleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountChangeRoleListener {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final TelegramSubscriptionService telegramSubscriptionService;

    @RabbitListener(queues = "#{@accountRoleQueue.name}")
    public void accountChangeRole(AccountRoleEvent event) {
        if (event.updateAt() == null) {              // atZone NPE 방지
            log.warn("updateAt 없는 role 이벤트 무시: {}", event);
            return;
        }
        telegramSubscriptionService.updateUserRole(event.userId(), event.role(), event.updateAt());
    }
}
