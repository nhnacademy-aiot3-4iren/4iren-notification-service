package com.siren.notificationservice.telegram.service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.config.TelegramSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramMessageServiceTest {

    private final TelegramSender adminTelegramSender = mock(TelegramSender.class);
    private final TelegramSender memberTelegramSender = mock(TelegramSender.class);
    private final TelegramMessageService telegramMessageService =
            new TelegramMessageService(adminTelegramSender, memberTelegramSender);

    @Test
    void sendMessageUsesAdminSenderForAdminBot() throws Exception {
        telegramMessageService.sendMessage("100", BotType.ADMIN_BOT, "안녕", "테스트");

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(adminTelegramSender).execute(captor.capture());
        assertThat(captor.getValue().getChatId()).isEqualTo("100");
        assertThat(captor.getValue().getText()).isEqualTo("안녕");
    }

    @Test
    void sendMessageUsesMemberSenderForUserBot() throws Exception {
        telegramMessageService.sendMessage("100", BotType.USER_BOT, "안녕", "테스트");

        verify(memberTelegramSender).execute(any(SendMessage.class));
    }

    @Test
    void sendMessageSwallowsExceptionWhenBotIsBlocked() throws Exception {
        TelegramApiRequestException blocked = mock(TelegramApiRequestException.class);
        when(blocked.getErrorCode()).thenReturn(403);
        doThrow(blocked).when(memberTelegramSender).execute(any(SendMessage.class));

        telegramMessageService.sendMessage("100", BotType.USER_BOT, "안녕", "테스트");

        verify(memberTelegramSender).execute(any(SendMessage.class));
    }

    @Test
    void sendMessageSwallowsGenericTelegramApiException() throws Exception {
        doThrow(new TelegramApiException("네트워크 오류")).when(memberTelegramSender).execute(any(SendMessage.class));

        telegramMessageService.sendMessage("100", BotType.USER_BOT, "안녕", "테스트");

        verify(memberTelegramSender).execute(any(SendMessage.class));
    }

    @Test
    void sendTokenExpiredMessageSendsGuideText() throws Exception {
        telegramMessageService.sendTokenExpiredMessage("100", BotType.USER_BOT);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(memberTelegramSender).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("만료");
    }

    @Test
    void answerCallbackAcknowledgesTap() throws Exception {
        telegramMessageService.answerCallback("cbq-1", BotType.USER_BOT);

        ArgumentCaptor<AnswerCallbackQuery> captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(memberTelegramSender).execute(captor.capture());
        assertThat(captor.getValue().getCallbackQueryId()).isEqualTo("cbq-1");
    }

    @Test
    void answerCallbackSwallowsException() throws Exception {
        doThrow(new TelegramApiException("실패")).when(memberTelegramSender).execute(any(AnswerCallbackQuery.class));

        telegramMessageService.answerCallback("cbq-1", BotType.USER_BOT);

        verify(memberTelegramSender).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void sendInlineKeyboardMessageBuildsOneButtonPerOption() throws Exception {
        telegramMessageService.sendInlineKeyboardMessage("100", BotType.USER_BOT, "고르세요",
                CallbackActionType.FEEDBACK_ROOM_SELECT, List.of("301호", "302호"));

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(memberTelegramSender).execute(captor.capture());
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) captor.getValue().getReplyMarkup();
        assertThat(markup.getKeyboard()).hasSize(2);
        assertThat(markup.getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("FB_ROOM:301호");
    }
}
