package com.siren.notificationservice.telegram.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

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

    /**
     * CallbackQuery.getMessage()의 타입(MaybeInaccessibleMessage)이 인터페이스라, Jackson이
     * 기본으로는 어떤 구현체로 만들지 못 정한다(telegrambots 라이브러리에 폴리모피즘 힌트가
     * 전혀 없음 - 실제 바이트코드로 확인함). 이게 없으면 인라인 키보드 콜백 웹훅(/webhook/*)이
     * JSON 역직렬화 단계에서 500으로 죽는다. Message로 매핑해서 해결한다.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer telegramCallbackJacksonCustomizer() {
        return builder -> builder.modulesToInstall(new SimpleModule()
                .addAbstractTypeMapping(MaybeInaccessibleMessage.class, Message.class));
    }
}
