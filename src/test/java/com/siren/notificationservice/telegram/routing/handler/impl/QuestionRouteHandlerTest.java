package com.siren.notificationservice.telegram.routing.handler.impl;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.client.RecommendationApiClient;
import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.cache.LastMentionedRoomService;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionRouteHandlerTest {

    private final RecommendationApiClient recommendationApiClient = mock(RecommendationApiClient.class);
    private final CoreApiClient coreApiClient = mock(CoreApiClient.class);
    private final LastMentionedRoomService lastMentionedRoomService = mock(LastMentionedRoomService.class);
    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final QuestionRouteHandler questionRouteHandler = new QuestionRouteHandler(
            recommendationApiClient, coreApiClient, lastMentionedRoomService, telegramMessageService);

    private TelegramInboundEvent textEvent(String text) {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText(text);
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    private UserRoomSubResponse subscribedRooms() {
        return new UserRoomSubResponse(1L, List.of(new UserRoomSubResponse.RoomSubResponse(7L, "301호", true)));
    }

    @Test
    void supportsReturnsQuestion() {
        assertThat(questionRouteHandler.supports()).isEqualTo(IntentType.QUESTION);
    }

    @Test
    void handleSendsCoreApiUnavailableMessageWhenCoreFails() {
        TelegramInboundEvent event = textEvent("몇 도야?");
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.empty());
        when(coreApiClient.getRoomSubscriptions(1L)).thenThrow(new CoreApiUnavailableException(1L, "userId"));

        questionRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendCoreApiUnavailableMessage(event.chatId(), BotType.USER_BOT);
        verify(recommendationApiClient, never()).getRecommendation(any(), any());
    }

    @Test
    void handleSendsInlineKeyboardWhenRecommendationHasOptions() {
        TelegramInboundEvent event = textEvent("환기는 언제 해?");
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.empty());
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        RecommendationResponse response = new RecommendationResponse(1L, 7L, "환기는 언제 해?", "지금 환기하세요",
                List.of("좋아요", "나중에"), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(recommendationApiClient.getRecommendation(any(), any())).thenReturn(response);

        questionRouteHandler.handle(event, 1L);

        verify(lastMentionedRoomService).save(1L, 7L);
        verify(telegramMessageService).sendInlineKeyboardMessage(event.chatId(), BotType.USER_BOT, "지금 환기하세요",
                CallbackActionType.QUESTION_CONTINUE, List.of("좋아요", "나중에"));
    }

    @Test
    void handleSendsPlainMessageWhenRecommendationHasNoOptions() {
        TelegramInboundEvent event = textEvent("몇 도야?");
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.empty());
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        RecommendationResponse response = new RecommendationResponse(1L, 7L, "몇 도야?", "지금 24도예요",
                List.of(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(recommendationApiClient.getRecommendation(any(), any())).thenReturn(response);

        questionRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendMessage(event.chatId(), BotType.USER_BOT, "지금 24도예요", "[Recommendation API] - LLM 답변");
    }

    @Test
    void handleDoesNotCacheRoomWhenRecommendationHasNoRoomId() {
        TelegramInboundEvent event = textEvent("공기질 어때?");
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.empty());
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        RecommendationResponse response = new RecommendationResponse(1L, null, "공기질 어때?", "잘 모르겠어요",
                List.of(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(recommendationApiClient.getRecommendation(any(), any())).thenReturn(response);

        questionRouteHandler.handle(event, 1L);

        verify(lastMentionedRoomService, never()).save(any(), any());
    }

    @Test
    void handlePassesLastMentionedRoomIntoRecommendationRequest() {
        TelegramInboundEvent event = textEvent("거기 습도 어때?");
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.of(7L));
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        RecommendationResponse response = new RecommendationResponse(1L, 7L, "거기 습도 어때?", "습도 55%예요",
                List.of(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(recommendationApiClient.getRecommendation(any(), any())).thenReturn(response);

        questionRouteHandler.handle(event, 1L);

        verify(recommendationApiClient).getRecommendation(1L, new RecommendationRequest(7L, List.of(7L), "거기 습도 어때?", event.requestAt()));
    }
}
