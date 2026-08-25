package com.siren.notificationservice.telegram.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

public final class TelegramInboundEvent {
    private static final String CALLBACK_DELIMITER = ":";
    private static final ZoneId zoneId = ZoneId.of("Asia/Seoul");

    private final BotType botType;
    private final Update update;
    private final String chatId;
    private final String question;
    private final LocalDateTime requestAt;
    private final CallbackActionType callbackActionType;

    @JsonCreator
    public TelegramInboundEvent(@JsonProperty("botType") BotType botType,@JsonProperty("update") Update update) {
        this.botType = botType;
        this.update = update;

        if(update.hasCallbackQuery()) {
            CallbackQuery cq = update.getCallbackQuery();
            this.chatId = cq.getMessage().getChatId().toString();

            String data = cq.getData(); //"FB_ROOM:301호"
            int idx = data.indexOf(CALLBACK_DELIMITER); // 7
            this.question = idx >= 0 ? data.substring(idx + 1) : data;
            String prefix = idx >= 0 ? data.substring(0, idx) : data; // 0-7까지 FB_ROOM
            this.callbackActionType = CallbackActionType.fromPrefix(prefix).orElse(null);
            this.requestAt = LocalDateTime.now(zoneId);
        }else if(update.hasMyChatMember()){
            this.chatId = update.getMyChatMember().getChat().getId().toString();
            this.question = null;
            this.callbackActionType = null;
            this.requestAt = LocalDateTime.ofEpochSecond(update.getMyChatMember().getDate(), 0, ZoneOffset.ofHours(9));
        }else if(update.hasMessage()) {
            this.chatId = update.getMessage().getChatId().toString();
            this.question = update.getMessage().getText();
            this.callbackActionType = null;
            this.requestAt = LocalDateTime.ofEpochSecond(update.getMessage().getDate(), 0, ZoneOffset.ofHours(9));
        }else{
            this.chatId = null;
            this.question = null;
            this.callbackActionType = null;
            this.requestAt = LocalDateTime.now(zoneId);
        }
    }

    // RabbitMQ 발행/구독 시 Jackson이 이 두 개만 실제 필드로 직렬화/역직렬화한다 —
    // 생성자의 @JsonProperty는 역직렬화(생성)만 담당하고, 직렬화(쓰기)는 게터 쪽에도
    // 똑같이 붙여줘야 Jackson이 "쓸 방법"을 찾는다 (get 접두어가 없는 메서드라 자동 인식 안 됨).
    @JsonProperty("botType")
    public BotType botType() { return botType; }

    @JsonProperty("update")
    public Update update() { return update; }

    // 아래 넷은 생성자에서 파생시킨 값이라 와이어 포맷에 실을 필요 없음 - 명시적으로 제외
    @JsonIgnore
    public String chatId() { return chatId; }

    @JsonIgnore
    public String question() { return question; }

    @JsonIgnore
    public LocalDateTime requestAt() { return requestAt; }

    @JsonIgnore
    public Optional<CallbackActionType> callbackActionType() { return Optional.ofNullable(callbackActionType); }
}
