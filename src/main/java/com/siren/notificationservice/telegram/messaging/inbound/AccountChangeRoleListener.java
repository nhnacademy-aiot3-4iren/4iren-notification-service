package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.exception.InvalidAccountRoleEventException;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.dto.event.AccountRoleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountChangeRoleListener {
    private final TelegramSubscriptionService telegramSubscriptionService;

    /**
     * updateAt이 없으면 재시도해도 결과가 달라지지 않으므로 삼키지 않고 던져서 재시도→DLQ로 보낸다.
     */
    @RabbitListener(queues = "#{@accountRoleQueue.name}")
    public void accountChangeRole(AccountRoleEvent event) {
        if (event.updateAt() == null) {
            throw new InvalidAccountRoleEventException("updateAt 없는 role 이벤트: " + event);
        }
        telegramSubscriptionService.updateUserRole(event.userId(), event.role(), event.updateAt());
    }
}
