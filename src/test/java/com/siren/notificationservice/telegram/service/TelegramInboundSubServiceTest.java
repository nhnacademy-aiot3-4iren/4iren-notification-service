package com.siren.notificationservice.telegram.service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import com.siren.notificationservice.telegram.dto.LinkTokenData;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramInboundSubServiceTest {

    private final TelegramSubscriptionRepository telegramSubscriptionRepository = mock(TelegramSubscriptionRepository.class);
    private final TelegramInboundSubService telegramInboundSubService =
            new TelegramInboundSubService(telegramSubscriptionRepository);

    private TelegramInboundEvent startEvent(String chatId) {
        Chat chat = new Chat();
        chat.setId(Long.parseLong(chatId));
        Message message = new Message();
        message.setChat(chat);
        message.setText("/start abc");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    private TelegramInboundEvent myChatMemberEvent(String chatId, ChatMember newStatus) {
        Chat chat = new Chat();
        chat.setId(Long.parseLong(chatId));
        ChatMemberUpdated chatMemberUpdated = new ChatMemberUpdated();
        chatMemberUpdated.setChat(chat);
        chatMemberUpdated.setNewChatMember(newStatus);
        chatMemberUpdated.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMyChatMember(chatMemberUpdated);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    @Test
    void handleValidStartLinksExistingSubscription() {
        TelegramSubscription existing = TelegramSubscription.builder()
                .userId(1L).botType(BotType.USER_BOT).chatId("old-chat").active(false).createdAt(LocalDateTime.now()).build();
        when(telegramSubscriptionRepository.findByUserIdAndBotType(1L, BotType.USER_BOT)).thenReturn(Optional.of(existing));
        TelegramInboundEvent event = startEvent("100");

        telegramInboundSubService.handleValidStart(event, new LinkTokenData(1L, UserRole.NORMAL));

        assertThat(existing.getChatId()).isEqualTo("100");
        assertThat(existing.isActive()).isTrue(); // link()가 재연동 시 active를 다시 켠다(봉쇄 후 딥링크 재연동 복구)
    }

    @Test
    void handleValidStartCreatesNewSubscriptionWhenNoneExists() {
        when(telegramSubscriptionRepository.findByUserIdAndBotType(1L, BotType.USER_BOT)).thenReturn(Optional.empty());
        TelegramInboundEvent event = startEvent("100");

        telegramInboundSubService.handleValidStart(event, new LinkTokenData(1L, UserRole.NORMAL));

        verify(telegramSubscriptionRepository).save(argThat(sub ->
                sub.getUserId().equals(1L) && sub.getChatId().equals("100") && sub.isActive()));
    }

    @Test
    void handleBlockedBotDeactivatesSubscriptionWhenBanned() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .userId(1L).botType(BotType.USER_BOT).chatId("100").active(true).createdAt(LocalDateTime.now()).build();
        when(telegramSubscriptionRepository.findByChatIdAndBotType("100", BotType.USER_BOT)).thenReturn(Optional.of(subscription));
        TelegramInboundEvent event = myChatMemberEvent("100", new ChatMemberBanned());

        telegramInboundSubService.handleBlockedBot(event);

        assertThat(subscription.isActive()).isFalse();
    }

    @Test
    void handleBlockedBotReactivatesSubscriptionWhenUnblocked() {
        TelegramSubscription subscription = TelegramSubscription.builder()
                .userId(1L).botType(BotType.USER_BOT).chatId("100").active(false).createdAt(LocalDateTime.now()).build();
        when(telegramSubscriptionRepository.findByChatIdAndBotType("100", BotType.USER_BOT)).thenReturn(Optional.of(subscription));
        TelegramInboundEvent event = myChatMemberEvent("100", new ChatMemberMember());

        telegramInboundSubService.handleBlockedBot(event);

        assertThat(subscription.isActive()).isTrue();
    }

    @Test
    void handleBlockedBotDoesNothingWhenSubscriptionNotFound() {
        when(telegramSubscriptionRepository.findByChatIdAndBotType("100", BotType.USER_BOT)).thenReturn(Optional.empty());
        TelegramInboundEvent event = myChatMemberEvent("100", new ChatMemberBanned());

        telegramInboundSubService.handleBlockedBot(event);

        // 예외 없이 조용히 넘어가야 한다 - 조회만 하고 그 이상 손댈 대상이 없다는 것 자체가 증거
        verify(telegramSubscriptionRepository).findByChatIdAndBotType("100", BotType.USER_BOT);
    }
}
