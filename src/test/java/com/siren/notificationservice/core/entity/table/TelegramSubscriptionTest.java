package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.BotType;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramSubscriptionTest {

    @Test
    void blockSetsActiveFalse() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .botType(BotType.ADMIN_BOT)
                .chatId("111")
                .active(true)
                .createdAt(ZonedDateTime.now())
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
                .createdAt(ZonedDateTime.now())
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
                .build();
        ZonedDateTime linkedAt = ZonedDateTime.now();

        subscription.link("222", linkedAt);

        assertThat(subscription.getChatId()).isEqualTo("222");
        assertThat(subscription.getCreatedAt()).isEqualTo(linkedAt);
    }
}
