package com.siren.notificationservice.telegram.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TelegramClientConfig {

    /**
     * Admin 봇 전용 발신 sender.
     *
     * @param properties telegram.admin-bot.* 바인딩 값
     * @return Admin 봇 토큰으로 초기화된 TelegramSender
     */
    @Bean
    public TelegramSender adminTelegramSender(TelegramBotProperties properties) {
        return new TelegramSender(new DefaultBotOptions(), properties.adminBot().token());
    }

    /**
     * Member 봇 전용 발신 sender.
     *
     * @param properties telegram.member-bot.* 바인딩 값
     * @return Member 봇 토큰으로 초기화된 TelegramSender
     */
    @Bean
    public TelegramSender memberTelegramSender(TelegramBotProperties properties) {
        return new TelegramSender(new DefaultBotOptions(), properties.memberBot().token());
    }
}
