package com.siren.notificationservice.telegram.config;

import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;

/**
 * DefaultAbsSender는 abstract이지만 실제로 남아있는 미구현 메서드가 없다 —
 * TelegramLongPollingBot/TelegramWebhookBot처럼 봇 인터페이스를 구현하지 않고
 * "메시지 발송 전용" 클라이언트로만 쓰기 위한 최소 구현체.
 * execute(SendMessage), execute(SetWebhook) 등은 부모(AbsSender)의 public 제네릭
 * execute(Method)로 그대로 쓸 수 있다.
 */
public class TelegramSender extends DefaultAbsSender {

    /**
     * @param options  텔레그램 API 연결 옵션 (프록시 등 필요 시 확장)
     * @param botToken 이 sender가 사용할 봇 토큰 (Admin/Member 봇 중 하나)
     */
    public TelegramSender(DefaultBotOptions options, String botToken) {
        super(options, botToken);
    }
}
