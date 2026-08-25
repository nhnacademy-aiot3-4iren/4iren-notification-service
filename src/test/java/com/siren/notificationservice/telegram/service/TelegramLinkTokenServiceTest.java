package com.siren.notificationservice.telegram.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.config.TelegramBotProperties;
import com.siren.notificationservice.telegram.dto.LinkTokenData;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class TelegramLinkTokenServiceTest {

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final TelegramSubscriptionService telegramSubscriptionService = mock(TelegramSubscriptionService.class);
    private final TelegramBotProperties telegramBotProperties = new TelegramBotProperties(
            new TelegramBotProperties.BotCredentials("admin-token", "admin_bot"),
            new TelegramBotProperties.BotCredentials("member-token", "member_bot"),
            new TelegramBotProperties.WebHook("https://example.com")
    );
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TelegramLinkTokenService telegramLinkTokenService =
            new TelegramLinkTokenService(stringRedisTemplate, telegramSubscriptionService, telegramBotProperties, objectMapper);

    @Test
    void getDeepLinkUrlBuildsUrlAndIssuesToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String url = telegramLinkTokenService.getDeepLinkUrl(new LinkTokenData(1L, UserRole.ADMIN), BotType.ADMIN_BOT);

        assertThat(url).startsWith("https://t.me/admin_bot?start=");
        verify(valueOperations).set(
                argThat(key -> key.startsWith("telegram:link-token:ADMIN_BOT:")),
                argThat(value -> value.contains("\"userId\":1")), eq(Duration.ofMinutes(5)));
    }

    @Test
    void getRedirectUrlReturnsPlainUrlWhenAlreadyLinked() {
        when(telegramSubscriptionService.isLinked(1L, BotType.USER_BOT)).thenReturn(true);

        String url = telegramLinkTokenService.getRedirectUrl(1L, BotType.USER_BOT);

        assertThat(url).isEqualTo("https://t.me/member_bot");
    }

    @Test
    void getRedirectUrlIssuesNewTokenWhenNotLinked() {
        when(telegramSubscriptionService.isLinked(1L, BotType.USER_BOT)).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String url = telegramLinkTokenService.getRedirectUrl(1L, BotType.USER_BOT);

        assertThat(url).startsWith("https://t.me/member_bot?start=");
    }

    @Test
    void consumeTokenReturnsUserIdAndDeletesToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("telegram:link-token:USER_BOT:abc"))
                .thenReturn("{\"userId\":1,\"role\":\"NORMAL\"}");

        Optional<LinkTokenData> data = telegramLinkTokenService.consumeToken("abc", BotType.USER_BOT);

        assertThat(data).isPresent();
        assertThat(data.get().userId()).isEqualTo(1L);
        assertThat(data.get().role()).isEqualTo(UserRole.NORMAL);
    }

    @Test
    void consumeTokenReturnsEmptyWhenTokenMissing() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("telegram:link-token:USER_BOT:abc")).thenReturn(null);

        Optional<LinkTokenData> data = telegramLinkTokenService.consumeToken("abc", BotType.USER_BOT);

        assertThat(data).isEmpty();
    }

    @Test
    void consumeTokenReturnsEmptyWhenStoredValueIsCorrupted() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("telegram:link-token:USER_BOT:abc")).thenReturn("깨진값");

        Optional<LinkTokenData> data = telegramLinkTokenService.consumeToken("abc", BotType.USER_BOT);

        assertThat(data).isEmpty();
    }
}
