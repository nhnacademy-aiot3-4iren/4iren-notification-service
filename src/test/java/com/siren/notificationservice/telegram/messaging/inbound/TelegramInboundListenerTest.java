package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.exception.MissingChatIdException;
import com.siren.notificationservice.core.exception.TelegramSubscriptionNotFoundException;
import com.siren.notificationservice.telegram.agent.IntentClassificationAgent;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.callback.handler.CallbackRouteDispatcher;
import com.siren.notificationservice.telegram.dto.LinkTokenData;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.service.TelegramInboundSubService;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramInboundListenerTest {

    private final TelegramLinkTokenService telegramLinkTokenService = mock(TelegramLinkTokenService.class);
    private final TelegramInboundSubService telegramInboundSubService = mock(TelegramInboundSubService.class);
    private final IntentClassificationAgent intentClassificationAgent = mock(IntentClassificationAgent.class);
    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final CallbackRouteDispatcher callbackRouteDispatcher = mock(CallbackRouteDispatcher.class);
    private final TelegramInboundListener telegramInboundListener = new TelegramInboundListener(
            telegramLinkTokenService, telegramInboundSubService, intentClassificationAgent,
            telegramMessageService, callbackRouteDispatcher);

    private TelegramInboundEvent textEvent(BotType botType, String text) {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText(text);
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(botType, update);
    }

    private TelegramInboundEvent unsupportedContentEvent(BotType botType) {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(botType, update);
    }

    private TelegramInboundEvent myChatMemberEvent(BotType botType) {
        Chat chat = new Chat();
        chat.setId(100L);
        ChatMemberUpdated chatMemberUpdated = new ChatMemberUpdated();
        chatMemberUpdated.setChat(chat);
        chatMemberUpdated.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMyChatMember(chatMemberUpdated);
        return new TelegramInboundEvent(botType, update);
    }

    private TelegramInboundEvent callbackEvent(BotType botType, String callbackData) {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cbq-1");
        callbackQuery.setMessage(message);
        callbackQuery.setData(callbackData);
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return new TelegramInboundEvent(botType, update);
    }

    @Test
    void handleWithValidStartTokenLinksAndSendsSuccessMessage() {
        TelegramInboundEvent event = textEvent(BotType.USER_BOT, "/start abc123");
        Long userId = 1L;
        UserRole role = UserRole.OWNER;
        LinkTokenData linkTokenData = new LinkTokenData(userId, role);
        when(telegramLinkTokenService.consumeToken("abc123", BotType.USER_BOT)).thenReturn(Optional.of(linkTokenData));

        telegramInboundListener.handle(event);

        verify(telegramInboundSubService).handleValidStart(event, linkTokenData);
        verify(telegramMessageService).sendLinkSuccessMessage(event.chatId(), BotType.USER_BOT);
    }

    @Test
    void handleWithExpiredStartTokenSendsExpiredMessage() {
        TelegramInboundEvent event = textEvent(BotType.USER_BOT, "/start expired");
        when(telegramLinkTokenService.consumeToken("expired", BotType.USER_BOT)).thenReturn(Optional.empty());

        telegramInboundListener.handle(event);

        verify(telegramMessageService).sendTokenExpiredMessage(event.chatId(), BotType.USER_BOT);
        verify(telegramInboundSubService, never()).handleValidStart(any(), any());
    }

    @Test
    void handleWithBlankStartTokenDoesNothing() {
        TelegramInboundEvent event = textEvent(BotType.USER_BOT, "/start");

        telegramInboundListener.handle(event);

        verify(telegramLinkTokenService, never()).consumeToken(anyString(), any());
        verify(telegramMessageService, never()).sendTokenExpiredMessage(anyString(), any());
    }

    @Test
    void handleWithMyChatMemberUpdatesBlockStatus() {
        TelegramInboundEvent event = myChatMemberEvent(BotType.ADMIN_BOT);

        telegramInboundListener.handle(event);

        verify(telegramInboundSubService).handleBlockedBot(event);
    }

    @Test
    void handleFreeTextFromAdminBotRedirectsToUserBot() {
        TelegramInboundEvent event = textEvent(BotType.ADMIN_BOT, "몇 도야?");
        when(telegramLinkTokenService.getUserIdByChatId(event.chatId(), BotType.ADMIN_BOT)).thenReturn(1L);
        when(telegramLinkTokenService.getRedirectUrl(1L, BotType.USER_BOT)).thenReturn("https://t.me/member_bot");

        telegramInboundListener.handle(event);

        verify(telegramMessageService).sendRedirectToUserBotMessage(event.chatId(), "https://t.me/member_bot");
        verify(intentClassificationAgent, never()).classify(any(), any());
    }

    @Test
    void handleFreeTextFromUserBotClassifiesIntent() {
        TelegramInboundEvent event = textEvent(BotType.USER_BOT, "너무 더워요");
        when(telegramLinkTokenService.getUserIdByChatId(event.chatId(), BotType.USER_BOT)).thenReturn(1L);

        telegramInboundListener.handle(event);

        verify(intentClassificationAgent).classify(event, 1L);
    }

    @Test
    void handleFreeTextFromUnlinkedUserSendsGuideMessage() {
        TelegramInboundEvent event = textEvent(BotType.USER_BOT, "너무 더워요");
        when(telegramLinkTokenService.getUserIdByChatId(event.chatId(), BotType.USER_BOT))
                .thenThrow(new TelegramSubscriptionNotFoundException());

        telegramInboundListener.handle(event);

        verify(telegramMessageService).sendNotLinkedGuideMessage(event.chatId(), BotType.USER_BOT);
        verify(intentClassificationAgent, never()).classify(any(), any());
    }

    @Test
    void handleFreeTextWithMissingChatIdOnlyLogsAndSendsNoMessage() {
        TelegramInboundEvent event = textEvent(BotType.USER_BOT, "너무 더워요");
        when(telegramLinkTokenService.getUserIdByChatId(event.chatId(), BotType.USER_BOT))
                .thenThrow(new MissingChatIdException());

        telegramInboundListener.handle(event);

        verify(telegramMessageService, never()).sendNotLinkedGuideMessage(anyString(), any());
        verify(intentClassificationAgent, never()).classify(any(), any());
    }

    @Test
    void handleUnsupportedContentSendsUnsupportedMessage() {
        TelegramInboundEvent event = unsupportedContentEvent(BotType.USER_BOT);

        telegramInboundListener.handle(event);

        verify(telegramMessageService).sendUnsupportedContentMessage(event.chatId(), BotType.USER_BOT);
    }

    @Test
    void handleCallbackQueryDispatchesToMatchingHandler() {
        TelegramInboundEvent event = callbackEvent(BotType.USER_BOT, "FB_ROOM:301호");
        when(telegramLinkTokenService.getUserIdByChatId(event.chatId(), BotType.USER_BOT)).thenReturn(1L);

        telegramInboundListener.handle(event);

        verify(telegramMessageService).answerCallback("cbq-1", BotType.USER_BOT);
        verify(callbackRouteDispatcher).dispatch(CallbackActionType.FEEDBACK_ROOM_SELECT, event, 1L);
    }

    @Test
    void handleCallbackQueryWithUnknownPrefixDoesNotDispatch() {
        TelegramInboundEvent event = callbackEvent(BotType.USER_BOT, "UNKNOWN_PREFIX:301호");
        when(telegramLinkTokenService.getUserIdByChatId(event.chatId(), BotType.USER_BOT)).thenReturn(1L);

        telegramInboundListener.handle(event);

        verify(callbackRouteDispatcher, never()).dispatch(any(), any(), any());
    }
}
