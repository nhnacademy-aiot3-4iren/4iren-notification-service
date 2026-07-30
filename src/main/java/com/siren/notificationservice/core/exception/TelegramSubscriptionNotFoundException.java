package com.siren.notificationservice.core.exception;

/**
 * 주어진 chatId/botType 조합으로 연동된 TelegramSubscription을 찾을 수 없을 때 발생한다.
 */
public class TelegramSubscriptionNotFoundException extends NotificationServiceException {

    public TelegramSubscriptionNotFoundException() {
        super(ErrorCode.TELEGRAM_SUBSCRIPTION_NOT_FOUND);
    }
}
