package com.siren.notificationservice.telegram.dto.event;

import com.siren.notificationservice.core.entity.domain.BotType;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record TelegramInboundEvent(
        BotType botType,
        Update update
) {
    /**
     * @return 유저의 채팅방 아이디
     */
    public String chatId() {
        return update.getMessage().getChatId().toString();
    }

    /**
     * @return 유저가 보낸 메세지
     */
    public String question(){
        return update.getMessage().getText();
    }

    /**
     * @return 유저가 메시지 보낸 시간
     */
    public LocalDateTime requestAt(){
        return LocalDateTime.ofEpochSecond(update.getMessage().getDate(), 0, ZoneOffset.ofHours(9));
    }
}
