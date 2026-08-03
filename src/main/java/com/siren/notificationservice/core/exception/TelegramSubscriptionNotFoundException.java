package com.siren.notificationservice.core.exception;

/**
 * 주어진 chatId/botType 조합으로 연동된 TelegramSubscription을 찾을 수 없을 때 발생한다.
 */
public class TelegramSubscriptionNotFoundException extends RuntimeException {

    public TelegramSubscriptionNotFoundException() {
        super("해당 chatId로 연동된 텔레그램 구독 정보를 찾을 수 없습니다.");
    }
}
