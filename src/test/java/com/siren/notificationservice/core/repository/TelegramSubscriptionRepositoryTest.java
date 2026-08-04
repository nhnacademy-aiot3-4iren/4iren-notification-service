package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class TelegramSubscriptionRepositoryTest {

    @Autowired
    private TelegramSubscriptionRepository telegramSubscriptionRepository;

    @Test
    void findsOnlyActiveSubscriptionsForBotType() {
        telegramSubscriptionRepository.save(subscription(1L, BotType.USER_BOT, "111", true));
        telegramSubscriptionRepository.save(subscription(2L, BotType.USER_BOT, "222", false));
        telegramSubscriptionRepository.save(subscription(3L, BotType.ADMIN_BOT, "333", true));

        List<TelegramSubscription> found = telegramSubscriptionRepository
                .findByUserIdInAndBotTypeAndActiveTrue(List.of(1L, 2L, 3L), BotType.USER_BOT);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void findsNothingWhenNoActiveSubscriptionMatches() {
        telegramSubscriptionRepository.save(subscription(1L, BotType.USER_BOT, "111", false));

        List<TelegramSubscription> found = telegramSubscriptionRepository
                .findByUserIdInAndBotTypeAndActiveTrue(List.of(1L), BotType.USER_BOT);

        assertThat(found).isEmpty();
    }

    @Test
    void findsSubscriptionByUserIdAndBotType() {
        telegramSubscriptionRepository.save(subscription(1L, BotType.USER_BOT, "111", true));

        Optional<TelegramSubscription> found = telegramSubscriptionRepository
                .findByUserIdAndBotType(1L, BotType.USER_BOT);

        assertThat(found).isPresent();
        assertThat(found.get().getChatId()).isEqualTo("111");
    }

    @Test
    void findsEmptyWhenUserNeverLinkedThatBot() {
        Optional<TelegramSubscription> found = telegramSubscriptionRepository
                .findByUserIdAndBotType(999L, BotType.USER_BOT);

        assertThat(found).isEmpty();
    }

    @Test
    void existsReturnsTrueWhenAlreadyLinked() {
        telegramSubscriptionRepository.save(subscription(1L, BotType.USER_BOT, "111", true));

        boolean exists = telegramSubscriptionRepository.existsByUserIdAndBotType(1L, BotType.USER_BOT);

        assertThat(exists).isTrue();
    }

    @Test
    void existsReturnsFalseWhenNeverLinked() {
        boolean exists = telegramSubscriptionRepository.existsByUserIdAndBotType(1L, BotType.USER_BOT);

        assertThat(exists).isFalse();
    }

    private TelegramSubscription subscription(Long userId, BotType botType, String chatId, boolean active) {
        return TelegramSubscription.builder()
                .userId(userId)
                .botType(botType)
                .chatId(chatId)
                .active(active)
                .createdAt(ZonedDateTime.now())
                .build();
    }
}
