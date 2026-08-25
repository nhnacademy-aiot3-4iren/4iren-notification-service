package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.exception.InvalidAccountRoleEventException;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.dto.event.AccountRoleEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountChangeRoleListenerTest {

    private final TelegramSubscriptionService telegramSubscriptionService = mock(TelegramSubscriptionService.class);
    private final AccountChangeRoleListener accountChangeRoleListener =
            new AccountChangeRoleListener(telegramSubscriptionService);

    @Test
    void accountChangeRoleThrowsWhenUpdateAtIsMissing() {
        AccountRoleEvent event = new AccountRoleEvent(1L, "ADMIN", null);

        assertThatThrownBy(() -> accountChangeRoleListener.accountChangeRole(event))
                .isInstanceOf(InvalidAccountRoleEventException.class);
    }

    @Test
    void accountChangeRoleDelegatesToServiceWhenValid() {
        LocalDateTime updateAt = LocalDateTime.now();
        AccountRoleEvent event = new AccountRoleEvent(1L, "ADMIN", updateAt);

        accountChangeRoleListener.accountChangeRole(event);

        verify(telegramSubscriptionService).updateUserRole(1L, "ADMIN", updateAt);
    }
}
