package com.siren.notificationservice.telegram.dto.event;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramInboundEventTest {

    private Chat chat(long id) {
        Chat chat = new Chat();
        chat.setId(id);
        return chat;
    }

    @Test
    void callbackQueryFillsChatIdQuestionAndActionTypeFromData() {
        Message message = new Message();
        message.setChat(chat(100L));
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData("FB_ROOM:301호");
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        TelegramInboundEvent event = new TelegramInboundEvent(BotType.USER_BOT, update);

        assertThat(event.chatId()).isEqualTo("100");
        assertThat(event.question()).isEqualTo("301호");
        assertThat(event.callbackActionType()).contains(CallbackActionType.FEEDBACK_ROOM_SELECT);
    }

    @Test
    void callbackQueryWithUnknownPrefixLeavesActionTypeEmpty() {
        Message message = new Message();
        message.setChat(chat(100L));
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData("UNKNOWN:301호");
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        TelegramInboundEvent event = new TelegramInboundEvent(BotType.USER_BOT, update);

        assertThat(event.callbackActionType()).isEmpty();
    }

    @Test
    void callbackQueryWithoutDelimiterTreatsWholeDataAsPrefix() {
        Message message = new Message();
        message.setChat(chat(100L));
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData("FB_ROOM"); // 콜론 없이 접두어만 온 경우
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);

        TelegramInboundEvent event = new TelegramInboundEvent(BotType.USER_BOT, update);

        assertThat(event.question()).isEqualTo("FB_ROOM");
        assertThat(event.callbackActionType()).contains(CallbackActionType.FEEDBACK_ROOM_SELECT);
    }

    @Test
    void myChatMemberFillsChatIdFromChatAndLeavesQuestionEmpty() {
        ChatMemberUpdated chatMemberUpdated = new ChatMemberUpdated();
        chatMemberUpdated.setChat(chat(200L));
        chatMemberUpdated.setDate(1690000000);
        Update update = new Update();
        update.setMyChatMember(chatMemberUpdated);

        TelegramInboundEvent event = new TelegramInboundEvent(BotType.ADMIN_BOT, update);

        assertThat(event.chatId()).isEqualTo("200");
        assertThat(event.question()).isNull();
        assertThat(event.callbackActionType()).isEmpty();
        assertThat(event.requestAt()).isEqualTo(LocalDateTime.ofEpochSecond(1690000000, 0, ZoneOffset.ofHours(9)));
    }

    @Test
    void messageFillsChatIdAndQuestionFromText() {
        Message message = new Message();
        message.setChat(chat(300L));
        message.setText("너무 더워요");
        message.setDate(1690000000);
        Update update = new Update();
        update.setMessage(message);

        TelegramInboundEvent event = new TelegramInboundEvent(BotType.USER_BOT, update);

        assertThat(event.chatId()).isEqualTo("300");
        assertThat(event.question()).isEqualTo("너무 더워요");
        assertThat(event.callbackActionType()).isEmpty();
        assertThat(event.requestAt()).isEqualTo(LocalDateTime.ofEpochSecond(1690000000, 0, ZoneOffset.ofHours(9)));
    }

    @Test
    void neitherCallbackNorChatMemberNorMessageLeavesChatIdAndQuestionNull() {
        Update update = new Update(); // poll, edited_message 등 우리가 처리 안 하는 update 타입 상황

        TelegramInboundEvent event = new TelegramInboundEvent(BotType.USER_BOT, update);

        assertThat(event.chatId()).isNull();
        assertThat(event.question()).isNull();
        assertThat(event.callbackActionType()).isEmpty();
        assertThat(event.requestAt()).isNotNull();
    }

    @Test
    void botTypeAndUpdateAreExposedAsGiven() {
        Message message = new Message();
        message.setChat(chat(100L));
        message.setText("안녕");
        message.setDate(1690000000);
        Update update = new Update();
        update.setMessage(message);

        TelegramInboundEvent event = new TelegramInboundEvent(BotType.ADMIN_BOT, update);

        assertThat(event.botType()).isEqualTo(BotType.ADMIN_BOT);
        assertThat(event.update()).isSameAs(update);
    }
}
