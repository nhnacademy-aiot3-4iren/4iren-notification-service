package com.siren.notificationservice.telegram.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramSetHook implements ApplicationRunner {
    private final TelegramBotProperties telegramBotProperties;
    private static final String SET_WEBHOOK_URL = "https://api.telegram.org/bot%s/setWebhook?url=%s";
    private static final String ADMIN_URL = "/webhook/admin";
    private static final String MEMBER_URL = "/webhook/member";
    private final RestClient restClient = RestClient.create();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        registerWebhook(telegramBotProperties.memberBot().token(), telegramBotProperties.webhook().baseUrl()+ MEMBER_URL);
        registerWebhook(telegramBotProperties.adminBot().token(), telegramBotProperties.webhook().baseUrl()+ ADMIN_URL);
    }

    private void registerWebhook(String token, String webhookUrl) {
        try{
            String encodedUrl = URLEncoder.encode(webhookUrl, StandardCharsets.UTF_8);
            String response = restClient.get()
                    .uri(String.format(SET_WEBHOOK_URL, token, encodedUrl))
                    .retrieve()
                    .body(String.class);
            log.info("[TelegramSetHook] 웹훅 등록 요청 (url={}) -> {}", webhookUrl, response);
        }catch (Exception e){
            log.warn("[TelegramSetHook] 웹훅 등록 실패 그래도 애플리케이션 부팅 진행 (url={})", webhookUrl, e);
        }
    }
}
