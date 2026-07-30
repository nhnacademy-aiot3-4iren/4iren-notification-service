package com.siren.notificationservice.core.exception;

/**
 * Telegram Update에 chatId가 존재하지 않을 때 발생한다.
 * 정상적인 메시지 Update라면 항상 chatId가 있어야 하므로, 이 예외는 예상치 못한 입력을 의미한다.
 */
public class MissingChatIdException extends NotificationServiceException {

    public MissingChatIdException() {
        super(ErrorCode.MISSING_CHAT_ID);
    }
}
