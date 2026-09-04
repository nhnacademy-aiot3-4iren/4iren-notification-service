package com.siren.notificationservice.telegram.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramClientConfigTest {

    private final TelegramClientConfig config = new TelegramClientConfig();

    private final TelegramBotProperties properties = new TelegramBotProperties(
            new TelegramBotProperties.BotCredentials("admin-token", "admin_bot"),
            new TelegramBotProperties.BotCredentials("member-token", "member_bot"),
            new TelegramBotProperties.WebHook("https://example.com")
    );

    @Test
    void adminTelegramSenderUsesAdminBotToken() {
        TelegramSender sender = config.adminTelegramSender(properties);

        assertThat(sender.getBaseUrl()).endsWith("admin-token/");
    }

    @Test
    void memberTelegramSenderUsesMemberBotToken() {
        TelegramSender sender = config.memberTelegramSender(properties);

        assertThat(sender.getBaseUrl()).endsWith("member-token/");
    }

    @Test
    void telegramCallbackJacksonCustomizerIsCreated() {
        Jackson2ObjectMapperBuilderCustomizer customizer = config.telegramCallbackJacksonCustomizer();

        assertThat(customizer).isNotNull();
    }
}
