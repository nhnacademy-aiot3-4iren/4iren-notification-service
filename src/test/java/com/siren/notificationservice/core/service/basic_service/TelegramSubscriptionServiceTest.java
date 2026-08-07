package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelegramSubscriptionServiceTest {

    private final TelegramSubscriptionRepository repository = mock(TelegramSubscriptionRepository.class);
    private final TelegramSubscriptionService service = new TelegramSubscriptionService(repository);

    @Test
    void findActiveAdminSubscriptionsQueriesAdminBotOnly() {
        List<Long> userIds = List.of(1L, 2L);
        List<TelegramSubscription> expected = List.of(mock(TelegramSubscription.class));
        when(repository.findByUserIdInAndBotTypeAndActiveTrue(userIds, BotType.ADMIN_BOT)).thenReturn(expected);

        assertThat(service.findActiveAdminSubscriptions(userIds)).isEqualTo(expected);
    }

    @Test
    void findActiveSubscriptionsQueriesAllBotTypes() {
        List<Long> userIds = List.of(1L, 2L);
        List<TelegramSubscription> expected = List.of(mock(TelegramSubscription.class));
        when(repository.findByUserIdInAndActiveTrue(userIds)).thenReturn(expected);

        assertThat(service.findActiveSubscriptions(userIds)).isEqualTo(expected);
    }
}
