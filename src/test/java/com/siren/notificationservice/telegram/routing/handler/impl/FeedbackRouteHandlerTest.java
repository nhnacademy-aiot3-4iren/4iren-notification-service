package com.siren.notificationservice.telegram.routing.handler.impl;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.dto.response.RoomSubResponse;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.FeedbackRoomResolver;
import com.siren.notificationservice.core.service.cache.FeedbackExtractionCacheService;
import com.siren.notificationservice.telegram.agent.FeedbackExtractionAgent;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import com.siren.notificationservice.telegram.messaging.outbound.FeedbackProcessingEventPublisher;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackRouteHandlerTest {

    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final FeedbackExtractionCacheService feedbackExtractionCacheService = mock(FeedbackExtractionCacheService.class);
    private final CoreApiClient coreApiClient = mock(CoreApiClient.class);
    private final FeedbackExtractionAgent feedbackExtractionAgent = mock(FeedbackExtractionAgent.class);
    private final FeedbackRoomResolver feedbackRoomResolver = mock(FeedbackRoomResolver.class);
    private final FeedbackProcessingEventPublisher feedbackProcessingEventPublisher = mock(FeedbackProcessingEventPublisher.class);
    private final FeedbackRouteHandler feedbackRouteHandler = new FeedbackRouteHandler(
            telegramMessageService, feedbackExtractionCacheService, coreApiClient,
            feedbackExtractionAgent, feedbackRoomResolver, feedbackProcessingEventPublisher);

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

    private UserRoomSubResponse subscribedRooms(int count) {
        List<RoomSubResponse> rooms = count == 1
                ? List.of(new RoomSubResponse(7L, "301호", true))
                : List.of(new RoomSubResponse(7L, "301호", true),
                        new RoomSubResponse(8L, "302호", true));
        return new UserRoomSubResponse(1L, rooms);
    }

    private FeedbackExtractionResult extractionResult(String mentionedRoomName) {
        return new FeedbackExtractionResult(
                List.of(new FeedbackExtractionResult.SensorScore(SensorType.TEMPERATURE, 2)),
                false, null, null, null, mentionedRoomName);
    }

    @Test
    void handleSendsCoreApiUnavailableMessageWhenCoreFails() {
        TelegramInboundEvent event = textEvent("너무 더워요");
        when(coreApiClient.getRoomSubscriptions(1L)).thenThrow(new CoreApiUnavailableException(1L, "userId"));

        feedbackRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendCoreApiUnavailableMessage(event.chatId(), BotType.USER_BOT);
        verify(feedbackExtractionAgent, never()).extract(any(), any());
    }

    @Test
    void handleSendsNoSubscribedRoomMessageWhenNoRoomsSubscribed() {
        TelegramInboundEvent event = textEvent("너무 더워요");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(new UserRoomSubResponse(1L, List.of()));

        feedbackRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendNoSubscribedRoomMessage(event.chatId(), BotType.USER_BOT);
    }

    @Test
    void handlePublishesAndAcknowledgesWhenRoomIsResolved() {
        TelegramInboundEvent event = textEvent("301호 너무 더워요");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms(2));
        when(feedbackExtractionAgent.extract(any(), any())).thenReturn(extractionResult("301호"));
        when(feedbackRoomResolver.resolve("301호", 1L, subscribedRooms(2).roomSubInfo())).thenReturn(Optional.of(7L));
        when(feedbackProcessingEventPublisher.publish(any())).thenReturn(true);

        feedbackRouteHandler.handle(event, 1L);

        verify(feedbackProcessingEventPublisher).publish(any());
        verify(telegramMessageService).sendFeedbackAcknowledgeMessage(event.chatId(), BotType.USER_BOT, "301호");
    }

    @Test
    void handleSendsFailedMessageWhenPublishFails() {
        TelegramInboundEvent event = textEvent("301호 너무 더워요");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms(2));
        when(feedbackExtractionAgent.extract(any(), any())).thenReturn(extractionResult("301호"));
        when(feedbackRoomResolver.resolve("301호", 1L, subscribedRooms(2).roomSubInfo())).thenReturn(Optional.of(7L));
        when(feedbackProcessingEventPublisher.publish(any())).thenReturn(false);

        feedbackRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendFeedbackProcessingFailedMessage(event.chatId(), BotType.USER_BOT);
        verify(telegramMessageService, never()).sendFeedbackAcknowledgeMessage(any(), any(), any());
    }

    @Test
    void handleAsksWhichRoomWhenAmbiguous() {
        TelegramInboundEvent event = textEvent("너무 더워요");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms(2));
        when(feedbackExtractionAgent.extract(any(), any())).thenReturn(extractionResult(null));
        when(feedbackRoomResolver.resolve(any(), anyLong(), any())).thenReturn(Optional.empty());
        when(feedbackExtractionCacheService.save(eq(1L), any())).thenReturn(true);

        feedbackRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendRoomDisambiguationAskMessage(event.chatId(), BotType.USER_BOT, List.of("301호", "302호"));
    }

    @Test
    void handleSendsFailedMessageWhenAmbiguousCacheSaveFails() {
        TelegramInboundEvent event = textEvent("너무 더워요");
        when(coreApiClient.getRoomSubscriptions(1L)).thenReturn(subscribedRooms(2));
        when(feedbackExtractionAgent.extract(any(), any())).thenReturn(extractionResult(null));
        when(feedbackRoomResolver.resolve(any(), anyLong(), any())).thenReturn(Optional.empty());
        when(feedbackExtractionCacheService.save(eq(1L), any())).thenReturn(false);

        feedbackRouteHandler.handle(event, 1L);

        verify(telegramMessageService).sendFeedbackProcessingFailedMessage(event.chatId(), BotType.USER_BOT);
        verify(telegramMessageService, never()).sendRoomDisambiguationAskMessage(any(), any(), any());
    }

    @Test
    void handleUserReplyProceedsWhenReplyMatchesCandidate() {
        TelegramInboundEvent event = textEvent("301호");
        FeedbackExtractionCache cache = new FeedbackExtractionCache("너무 더워요",
                List.of(new FeedbackExtractionCache.RoomCandidate(7L, "301호")), extractionResult(null));
        when(feedbackRoomResolver.matchReply("301호", 1L, cache.candidates())).thenReturn(Optional.of(7L));
        when(feedbackProcessingEventPublisher.publish(any())).thenReturn(true);

        feedbackRouteHandler.handleUserReply(event, 1L, cache);

        verify(feedbackExtractionCacheService).clear(1L);
        verify(telegramMessageService).sendFeedbackAcknowledgeMessage(event.chatId(), BotType.USER_BOT, "301호");
    }

    @Test
    void handleUserReplyRetriesWhenReplyMatchesNoCandidate() {
        TelegramInboundEvent event = textEvent("모르는방");
        FeedbackExtractionCache cache = new FeedbackExtractionCache("너무 더워요",
                List.of(new FeedbackExtractionCache.RoomCandidate(7L, "301호")), extractionResult(null));
        when(feedbackRoomResolver.matchReply("모르는방", 1L, cache.candidates())).thenReturn(Optional.empty());

        feedbackRouteHandler.handleUserReply(event, 1L, cache);

        verify(telegramMessageService).sendRoomDisambiguationRetryMessage(event.chatId(), BotType.USER_BOT, List.of("301호"));
        verify(feedbackExtractionCacheService, never()).clear(any());
    }
}
