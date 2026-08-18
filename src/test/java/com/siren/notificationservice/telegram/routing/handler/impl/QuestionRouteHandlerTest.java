package com.siren.notificationservice.telegram.routing.handler.impl;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.client.RecommendationApiClient;
import com.siren.notificationservice.core.dto.ConversationContext;
import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import com.siren.notificationservice.core.dto.response.RoomSubResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.core.service.cache.LastMentionedRoomService;
import com.siren.notificationservice.core.service.cache.LlmConversationContextService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionRouteHandlerTest {

    private final RecommendationApiClient recommendationApiClient = mock(RecommendationApiClient.class);
    private final TelegramSubscriptionService telegramSubscriptionService = mock(TelegramSubscriptionService.class);
    private final CoreApiClient coreApiClient = mock(CoreApiClient.class);
    private final LastMentionedRoomService lastMentionedRoomService = mock(LastMentionedRoomService.class);
    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final LlmConversationContextService llmConversationContextService = mock(LlmConversationContextService.class);
    private final QuestionRouteHandler questionRouteHandler = new QuestionRouteHandler(
            recommendationApiClient, telegramSubscriptionService, coreApiClient, lastMentionedRoomService, telegramMessageService,
            llmConversationContextService);

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

    private final RoomSubResponse room = new RoomSubResponse(7L, "301호", true);

    private UserRoomSubResponse subscribedRooms() {
        return new UserRoomSubResponse(1L, List.of(room));
    }

    private RecommendationResponse response(Long roomId, String answer, List<String> options) {
        return new RecommendationResponse(1L, roomId, "질문", new RecommendationResponse.Answer(answer, options),
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void supportsReturnsQuestion() {
        assertThat(questionRouteHandler.supports()).isEqualTo(IntentType.QUESTION);
    }

    @Test
    void handleSendsCoreApiUnavailableMessageWhenCoreFails() {
        TelegramInboundEvent event = textEvent("몇 도야?");
        when(coreApiClient.getRoomSubscriptions(1L)).thenThrow(new CoreApiUnavailableException(1L, "userId"));

        questionRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendCoreApiUnavailableMessage(event.chatId(), BotType.USER_BOT);
        verify(recommendationApiClient, never()).getRecommendation(any(), any(), any(), any());
    }

    @Test
    void handleSendsInlineKeyboardWhenRecommendationHasOptions() {
        TelegramInboundEvent event = textEvent("환기는 언제 해?");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        when(recommendationApiClient.getRecommendation(any(), any(), any(), any()))
                .thenReturn(response(7L, "지금 환기하세요", List.of("좋아요", "나중에")));

        questionRouteHandler.handle(event, 1L);

        verify(lastMentionedRoomService).save(1L, 7L);
        verify(telegramMessageService).sendInlineKeyboardMessage(event.chatId(), BotType.USER_BOT, "지금 환기하세요",
                CallbackActionType.QUESTION_CONTINUE, List.of("좋아요", "나중에"));
        verify(llmConversationContextService, never()).save(any(), any());
    }

    @Test
    void handleSendsPlainMessageWhenRecommendationHasNoOptions() {
        TelegramInboundEvent event = textEvent("몇 도야?");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        when(recommendationApiClient.getRecommendation(any(), any(), any(), any()))
                .thenReturn(response(7L, "지금 24도예요", List.of()));

        questionRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendMessage(event.chatId(), BotType.USER_BOT, "지금 24도예요", "[Recommendation API] - LLM 답변");
    }

    @Test
    void handleSavesConversationContextWhenRecommendationHasNoOptions() {
        TelegramInboundEvent event = textEvent("몇 도야?");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        when(recommendationApiClient.getRecommendation(any(), any(), any(), any()))
                .thenReturn(response(7L, "지금 24도예요", List.of()));

        questionRouteHandler.handle(event, 1L);

        verify(llmConversationContextService).save(1L, new ConversationContext("QUESTION", "몇 도야?", "지금 24도예요"));
    }

    @Test
    void handleDoesNotCacheRoomWhenRecommendationHasNoRoomId() {
        TelegramInboundEvent event = textEvent("공기질 어때?");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        when(recommendationApiClient.getRecommendation(any(), any(), any(), any()))
                .thenReturn(response(null, "잘 모르겠어요", List.of()));

        questionRouteHandler.handle(event, 1L);

        verify(lastMentionedRoomService, never()).save(any(), any());
    }

    @Test
    void handlePassesRoomSubInfoAndClientTypeIntoRecommendationRequest() {
        TelegramInboundEvent event = textEvent("거기 습도 어때?");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms());
        when(telegramSubscriptionService.getUserRole(1L)).thenReturn(UserRole.NORMAL);
        when(recommendationApiClient.getRecommendation(any(), any(), any(), any()))
                .thenReturn(response(7L, "습도 55%예요", List.of()));

        questionRouteHandler.handle(event, 1L);

        verify(recommendationApiClient).getRecommendation(1L, UserRole.NORMAL, "TELEGRAM",
                new RecommendationRequest(List.of(room), "거기 습도 어때?", event.requestAt()));
    }
}
