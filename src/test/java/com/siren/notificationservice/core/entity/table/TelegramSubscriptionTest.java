package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.BotType;
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
}
