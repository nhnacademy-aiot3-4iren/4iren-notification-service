package com.siren.notificationservice.telegram.service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.exception.MissingChatIdException;
import com.siren.notificationservice.core.exception.TelegramSubscriptionNotFoundException;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import com.siren.notificationservice.telegram.config.TelegramBotProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class TelegramLinkTokenServiceTest {

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final TelegramSubscriptionRepository telegramSubscriptionRepository = mock(TelegramSubscriptionRepository.class);
    private final TelegramBotProperties telegramBotProperties = new TelegramBotProperties(
            new TelegramBotProperties.BotCredentials("admin-token", "admin_bot"),
            new TelegramBotProperties.BotCredentials("member-token", "member_bot"),
            new TelegramBotProperties.WebHook("https://example.com")
    );
    private final TelegramLinkTokenService telegramLinkTokenService =
            new TelegramLinkTokenService(stringRedisTemplate, telegramSubscriptionRepository, telegramBotProperties);

    @Test
    void getDeepLinkUrlBuildsUrlAndIssuesToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String url = telegramLinkTokenService.getDeepLinkUrl(1L, BotType.ADMIN_BOT);

        assertThat(url).startsWith("https://t.me/admin_bot?start=");
        verify(valueOperations).set(
                argThat(key -> key.startsWith("telegram:link-token:ADMIN_BOT:")), eq("1"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void getRedirectUrlReturnsPlainUrlWhenAlreadyLinked() {
        when(telegramSubscriptionRepository.existsByUserIdAndBotType(1L, BotType.USER_BOT)).thenReturn(true);

        String url = telegramLinkTokenService.getRedirectUrl(1L, BotType.USER_BOT);

        assertThat(url).isEqualTo("https://t.me/member_bot");
    }

    @Test
    void getRedirectUrlIssuesNewTokenWhenNotLinked() {
        when(telegramSubscriptionRepository.existsByUserIdAndBotType(1L, BotType.USER_BOT)).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String url = telegramLinkTokenService.getRedirectUrl(1L, BotType.USER_BOT);

        assertThat(url).startsWith("https://t.me/member_bot?start=");
    }

    @Test
    void consumeTokenReturnsUserIdAndDeletesToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("telegram:link-token:USER_BOT:abc")).thenReturn("1");

        Optional<Long> userId = telegramLinkTokenService.consumeToken("abc", BotType.USER_BOT);

        assertThat(userId).contains(1L);
    }

    @Test
    void consumeTokenReturnsEmptyWhenTokenMissing() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("telegram:link-token:USER_BOT:abc")).thenReturn(null);

        Optional<Long> userId = telegramLinkTokenService.consumeToken("abc", BotType.USER_BOT);

        assertThat(userId).isEmpty();
    }

    @Test
    void consumeTokenReturnsEmptyWhenStoredValueIsNotANumber() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("telegram:link-token:USER_BOT:abc")).thenReturn("깨진값");

        Optional<Long> userId = telegramLinkTokenService.consumeToken("abc", BotType.USER_BOT);

        assertThat(userId).isEmpty();
    }

    @Test
    void isLinkedDelegatesToRepository() {
        when(telegramSubscriptionRepository.existsByUserIdAndBotType(1L, BotType.ADMIN_BOT)).thenReturn(true);

        assertThat(telegramLinkTokenService.isLinked(1L, BotType.ADMIN_BOT)).isTrue();
    }

    @Test
    void getUserIdByChatIdReturnsLinkedUserId() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .userId(5L).botType(BotType.USER_BOT).chatId("100").active(true).createdAt(ZonedDateTime.now()).build();
        when(telegramSubscriptionRepository.findByChatIdAndBotType("100", BotType.USER_BOT)).thenReturn(Optional.of(subscription));

        Long userId = telegramLinkTokenService.getUserIdByChatId("100", BotType.USER_BOT);

        assertThat(userId).isEqualTo(5L);
    }

    @Test
    void getUserIdByChatIdThrowsWhenChatIdIsNull() {
        assertThatThrownBy(() -> telegramLinkTokenService.getUserIdByChatId(null, BotType.USER_BOT))
                .isInstanceOf(MissingChatIdException.class);
    }

    @Test
    void getUserIdByChatIdThrowsWhenNotLinked() {
        when(telegramSubscriptionRepository.findByChatIdAndBotType("100", BotType.USER_BOT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telegramLinkTokenService.getUserIdByChatId("100", BotType.USER_BOT))
                .isInstanceOf(TelegramSubscriptionNotFoundException.class);
    }
}
