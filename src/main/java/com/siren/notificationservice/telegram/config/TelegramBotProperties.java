package com.siren.notificationservice.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public record TelegramBotProperties(
        BotCredentials adminBot,
        BotCredentials memberBot,
        WebHook webHook
) {
    public record BotCredentials(String token, String username) { }
    public record WebHook(String baseUrl){}
}
