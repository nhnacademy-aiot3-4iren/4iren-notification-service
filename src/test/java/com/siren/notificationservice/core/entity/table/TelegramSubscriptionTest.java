package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramSubscriptionTest {

    @Test
    void blockSetsActiveFalse() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .botType(BotType.ADMIN_BOT)
                .chatId("111")
                .active(true)
                .createdAt(LocalDateTime.now())
                .userId(1L)
                .build();

        subscription.block();

        assertThat(subscription.isActive()).isFalse();
    }

    @Test
    void unblockSetsActiveTrue() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .botType(BotType.ADMIN_BOT)
                .chatId("111")
                .active(false)
                .createdAt(LocalDateTime.now())
                .userId(1L)
                .build();

        subscription.unblock();

        assertThat(subscription.isActive()).isTrue();
    }

    @Test
    void linkUpdatesChatIdAndCreatedAt() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .botType(BotType.USER_BOT)
                .userId(1L)
                .active(true)
                .build();
        LocalDateTime linkedAt = LocalDateTime.now();

        subscription.link("222", linkedAt);

        assertThat(subscription.getChatId()).isEqualTo("222");
        assertThat(subscription.getCreatedAt()).isEqualTo(linkedAt);
    }

    @Test
    void linkReactivatesBlockedSubscription() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .userId(1L)
                .botType(BotType.USER_BOT)
                .chatId("111")
                .active(false) // 봇 차단된 상태
                .createdAt(LocalDateTime.now())
                .build();

        subscription.link("222", LocalDateTime.now());

        assertThat(subscription.isActive()).isTrue();
    }

    @Test
    void updateUserRoleAppliesNewerEvent() {
        LocalDateTime firstUpdate = LocalDateTime.now().minusMinutes(1);
        TelegramSubscription subscription = TelegramSubscription.builder()
                .botType(BotType.USER_BOT).chatId("111").active(true)
                .createdAt(LocalDateTime.now()).userId(1L)
                .userRole(UserRole.NORMAL).roleUpdatedAt(firstUpdate).build();
        LocalDateTime newerUpdate = firstUpdate.plusMinutes(1);

        subscription.updateUserRole(UserRole.ADMIN, newerUpdate);

        assertThat(subscription.getUserRole()).isEqualTo(UserRole.ADMIN);
        assertThat(subscription.getRoleUpdatedAt()).isEqualTo(newerUpdate);
    }

    @Test
    void updateUserRoleIgnoresStaleEvent() {
        LocalDateTime latestUpdate = LocalDateTime.now();
        TelegramSubscription subscription = TelegramSubscription.builder()
                .botType(BotType.USER_BOT).chatId("111").active(true)
                .createdAt(LocalDateTime.now()).userId(1L)
                .userRole(UserRole.ADMIN).roleUpdatedAt(latestUpdate).build();
        LocalDateTime staleUpdate = latestUpdate.minusMinutes(1);

        subscription.updateUserRole(UserRole.NORMAL, staleUpdate);

        assertThat(subscription.getUserRole()).isEqualTo(UserRole.ADMIN);
        assertThat(subscription.getRoleUpdatedAt()).isEqualTo(latestUpdate);
    }

    @Test
    void updateUserRoleIgnoresEventWithSameTimestamp() {
        LocalDateTime sameUpdate = LocalDateTime.now();
        TelegramSubscription subscription = TelegramSubscription.builder()
                .botType(BotType.USER_BOT).chatId("111").active(true)
                .createdAt(LocalDateTime.now()).userId(1L)
                .userRole(UserRole.ADMIN).roleUpdatedAt(sameUpdate).build();

        subscription.updateUserRole(UserRole.NORMAL, sameUpdate);

        assertThat(subscription.getUserRole()).isEqualTo(UserRole.ADMIN);
    }
}
