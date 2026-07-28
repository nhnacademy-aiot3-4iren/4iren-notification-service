package com.siren.notificationservice.telegram.dto.event;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public record TelegramInboundEvent(
        BotType botType,
        Update update
) {
    private static final String CALLBACK_DELIMITER = ":";
    /**
     * @return 유저의 채팅방 아이디
     */
    public String chatId() {
        if(update.hasCallbackQuery()){
            return update.getCallbackQuery().getMessage().getChatId().toString();
        }
        return update.getMessage().getChatId().toString();
    }

    /**
     * @return 유저가 보낸 메세지
     */
    public String question(){
        if(update.hasCallbackQuery()){
            String data = update.getCallbackQuery().getData();
            int idx = data.indexOf(CALLBACK_DELIMITER);
            return idx >=0 ? data.substring(idx+1) : data;
        }
        return update.getMessage().getText();
    }

    /** 콜백일 때만 값이 있음 — 라우팅 전용, 비즈니스 로직에선 안 씀 */
    public Optional<CallbackActionType> callbackActionType(){
        if(update.hasCallbackQuery()){
            return Optional.empty();
        }
        String data = update.getCallbackQuery().getData();
        int idx = data.indexOf(CALLBACK_DELIMITER);
        String prefix = idx >= 0 ? data.substring(0, idx) : data;
        return CallbackActionType.fromPrefix(prefix);
    }

    /**
     * @return 유저가 메시지 보낸 시간
     */
    public LocalDateTime requestAt(){
        return LocalDateTime.ofEpochSecond(update.getMessage().getDate(), 0, ZoneOffset.ofHours(9));
    }
}
