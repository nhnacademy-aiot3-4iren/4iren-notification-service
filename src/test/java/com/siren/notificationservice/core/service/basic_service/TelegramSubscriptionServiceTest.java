package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.exception.InvalidAccountRoleEventException;
import com.siren.notificationservice.core.exception.MissingChatIdException;
import com.siren.notificationservice.core.exception.TelegramSubscriptionNotFoundException;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void findActiveAdminSubscriptionsThrowsWhenUserIdsIsNull() {
        assertThatThrownBy(() -> service.findActiveAdminSubscriptions(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void findActiveSubscriptionsQueriesAllBotTypes() {
        List<Long> userIds = List.of(1L, 2L);
        List<TelegramSubscription> expected = List.of(mock(TelegramSubscription.class));
        when(repository.findByUserIdInAndActiveTrue(userIds)).thenReturn(expected);

        assertThat(service.findActiveSubscriptions(userIds)).isEqualTo(expected);
    }

    @Test
    void findActiveSubscriptionsThrowsWhenUserIdsIsNull() {
        assertThatThrownBy(() -> service.findActiveSubscriptions(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isLinkedDelegatesToRepository() {
        when(repository.existsByUserIdAndBotType(1L, BotType.ADMIN_BOT)).thenReturn(true);

        assertThat(service.isLinked(1L, BotType.ADMIN_BOT)).isTrue();
    }

    @Test
    void isLinkedThrowsWhenUserIdIsNull() {
        assertThatThrownBy(() -> service.isLinked(null, BotType.ADMIN_BOT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isLinkedThrowsWhenBotTypeIsNull() {
        assertThatThrownBy(() -> service.isLinked(1L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getUserIdByChatIdReturnsLinkedUserId() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .userId(5L).botType(BotType.USER_BOT).chatId("100").active(true).createdAt(LocalDateTime.now()).build();
        when(repository.findByChatIdAndBotType("100", BotType.USER_BOT)).thenReturn(Optional.of(subscription));

        Long userId = service.getUserIdByChatId("100", BotType.USER_BOT);

        assertThat(userId).isEqualTo(5L);
    }

    @Test
    void getUserIdByChatIdThrowsWhenChatIdIsNull() {
        assertThatThrownBy(() -> service.getUserIdByChatId(null, BotType.USER_BOT))
                .isInstanceOf(MissingChatIdException.class);
    }

    @Test
    void getUserIdByChatIdThrowsWhenNotLinked() {
        when(repository.findByChatIdAndBotType("100", BotType.USER_BOT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserIdByChatId("100", BotType.USER_BOT))
                .isInstanceOf(TelegramSubscriptionNotFoundException.class);
    }

    @Test
    void getUserIdByChatIdThrowsWhenBotTypeIsNull() {
        assertThatThrownBy(() -> service.getUserIdByChatId("100", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getTelegramSubscriptionsReturnsSubscriptionsForUser() {
        List<TelegramSubscription> expected = List.of(mock(TelegramSubscription.class));
        when(repository.findByUserId(1L)).thenReturn(expected);

        assertThat(service.getTelegramSubscriptions(1L)).isEqualTo(expected);
    }

    @Test
    void getTelegramSubscriptionsThrowsWhenUserIdIsNull() {
        assertThatThrownBy(() -> service.getTelegramSubscriptions(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateUserRoleThrowsWhenUserIdIsNull() {
        LocalDateTime updateAt = LocalDateTime.now();

        assertThatThrownBy(() -> service.updateUserRole(null, "ADMIN", updateAt))
                .isInstanceOf(InvalidAccountRoleEventException.class);
    }

    @Test
    void updateUserRoleThrowsWhenRoleIsNull() {
        LocalDateTime updateAt = LocalDateTime.now();

        assertThatThrownBy(() -> service.updateUserRole(1L, null, updateAt))
                .isInstanceOf(InvalidAccountRoleEventException.class);
    }

    @Test
    void updateUserRoleThrowsWhenRoleIsUnknown() {
        LocalDateTime updateAt = LocalDateTime.now();

        assertThatThrownBy(() -> service.updateUserRole(1L, "SUPER_ADMIN", updateAt))
                .isInstanceOf(InvalidAccountRoleEventException.class);
    }

    @Test
    void updateUserRoleUpdatesAllSubscriptionsForUser() {
        LocalDateTime updateAt = LocalDateTime.now();
        TelegramSubscription subscription = TelegramSubscription.builder()
                .userId(1L).botType(BotType.USER_BOT).chatId("100").active(true)
                .createdAt(LocalDateTime.now()).userRole(UserRole.NORMAL).build();
        when(repository.findByUserId(1L)).thenReturn(List.of(subscription));

        service.updateUserRole(1L, "ADMIN", updateAt);

        assertThat(subscription.getUserRole()).isEqualTo(UserRole.ADMIN);
    }
}
