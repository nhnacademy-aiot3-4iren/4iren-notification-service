package com.siren.notificationservice.telegram.routing.handler.impl;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.PendingUserReply;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.FeedbackRoomResolver;
import com.siren.notificationservice.core.service.PendingUserReplyService;
import com.siren.notificationservice.telegram.agent.FeedbackExtractionAgent;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.dto.feedback.ExperiencedTimeResolver;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import com.siren.notificationservice.telegram.messaging.outbound.FeedbackProcessingEventPublisher;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handler.IntentRouteHandler;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FeedbackRouteHandler implements IntentRouteHandler {
    private final TelegramMessageService telegramMessageService;
    private final PendingUserReplyService pendingUserReplyService;
    private final CoreApiClient coreApiClient;
    private final FeedbackExtractionAgent feedbackExtractionAgent;
    private final FeedbackRoomResolver feedbackRoomResolver;
    private final FeedbackProcessingEventPublisher feedbackProcessingEventPublisher;


    @Override
    public IntentType supports() {
        return IntentType.FEEDBACK;
    }

    @Override
    public void handle(TelegramInboundEvent event, Long userId) {
        String rawText = event.question();

        // 1. 구독 강의실 목록 조회 (Core API 실패 시 안내하고 종료)
        List<UserRoomSubResponse.RoomSubResponse> subscribedRooms;
        try {
            subscribedRooms = coreApiClient.getRoomSubscriptions(userId).roomSubInfo();
        } catch (CoreApiUnavailableException e) {
            telegramMessageService.sendCoreApiUnavailableMessage(event.chatId(), event.botType());
            return;
        }

        // 2. 구독한 강의실이 아예 없으면 안내하고 종료
        if (subscribedRooms.isEmpty()) {
            telegramMessageService.sendNoSubscribedRoomMessage(event.chatId(), event.botType());
            return;
        }

        // 2-1. 피드백 추출 - 구독 강의실 이름을 컨텍스트로 같이 넘겨서 강의실 언급 여부도 한 번에 판단
        List<String> subscribedRoomNames = subscribedRooms.stream()
                .map(UserRoomSubResponse.RoomSubResponse::roomName)
                .toList();
        FeedbackExtractionResult feedbackExtractionResult = feedbackExtractionAgent.extract(rawText, subscribedRoomNames);

        // 3. roomId를 후보 소스 순서대로 시도: 텍스트 언급 -> 마지막 언급 강의실 -> 구독 1개뿐인 경우
        Optional<Long> roomId = feedbackRoomResolver.resolve(feedbackExtractionResult.mentionedRoomName(), userId, subscribedRooms);

        // 4. 그래도 모호하면(구독 여러 개) 되묻고 대기 상태로 전환
        if (roomId.isEmpty()) {
            askWhichRoom(event, userId, rawText, subscribedRooms, feedbackExtractionResult);
            return;
        }

        // 5. 강의실 확정 - 이후 처리로 이어감
        proceedWithConfirmedRoom(event, userId, rawText, roomId.get(), feedbackExtractionResult);
    }

    /**
     * 되묻기 대기 중이던 유저의 답변(강의실 이름)을 처리한다.
     * 후보 중 정확히 하나만 매칭되면 대기 상태를 지우고 원본 피드백 처리를 이어가고,
     * 매칭 안 되거나 여러 개 매칭되면 다시 물어본다.
     *
     * @param event            원본 텔레그램 인바운드 이벤트 (강의실 이름 답변)
     * @param userId           채팅 유저
     * @param pendingUserReply 대기 중이던 원본 피드백 원문 + 후보 강의실 목록
     */
    public void handleUserReply(TelegramInboundEvent event, Long userId, PendingUserReply pendingUserReply) {

        Optional<Long> roomId = feedbackRoomResolver.matchReply(event.question(), userId,pendingUserReply.candidates());

        if (roomId.isEmpty()) {
            List<String> roomNames = pendingUserReply.candidates().stream()
                    .map(PendingUserReply.RoomCandidate::roomName)
                    .toList();
            telegramMessageService.sendRoomDisambiguationRetryMessage(event.chatId(), event.botType(), roomNames);
            return;
        }

        pendingUserReplyService.clear(userId);
        proceedWithConfirmedRoom(event, userId, pendingUserReply.rawText(), roomId.get(), pendingUserReply.feedbackExtractionResult());
    }


    /**
     * 강의실이 확정된 피드백을 큐로 넘기고 소프트 확언을 보낸다.
     * publish 실패(브로커 장애 등)나 체감 시각 조립 실패(LLM 출력이 유효 범위를 벗어난 경우)를 흡수한다 —
     * 이 메서드가 예외를 던지면 DLQ 없는 리스너 구조상 무한 재큐잉으로 이어지기 때문이다.
     */
    private void proceedWithConfirmedRoom(TelegramInboundEvent event, Long userId, String rawText, Long roomId, FeedbackExtractionResult feedbackExtractionResult) {
        ZonedDateTime receivedAt = event.requestAt().atZone(ZoneOffset.ofHours(9));
        ZonedDateTime experiencedAt = ExperiencedTimeResolver.resolve(feedbackExtractionResult, receivedAt);

        FeedbackProcessingEvent processingEvent = new FeedbackProcessingEvent(
                userId, roomId, rawText,
                feedbackExtractionResult.sensorScores(), feedbackExtractionResult.isDelayed(),
                experiencedAt, receivedAt
        );

        boolean isSuccess = feedbackProcessingEventPublisher.publish(processingEvent);

        if (!isSuccess) {
            telegramMessageService.sendFeedbackProcessingFailedMessage(event.chatId(), event.botType());
            return;
        }
        telegramMessageService.sendFeedbackAcknowledgeMessage(event.chatId(), event.botType());
    }


    private void askWhichRoom(TelegramInboundEvent event, Long userId, String rawText, List<UserRoomSubResponse.RoomSubResponse> rooms, FeedbackExtractionResult feedbackExtractionResult) {
        List<PendingUserReply.RoomCandidate> candidates = rooms.stream()
                .map(r -> new PendingUserReply.RoomCandidate(r.roomId(), r.roomName()))
                .toList();
        boolean saved = pendingUserReplyService.save(userId, new PendingUserReply(rawText, candidates, feedbackExtractionResult));

        if(!saved) {
            telegramMessageService.sendFeedbackProcessingFailedMessage(event.chatId(), event.botType());
            return;
        }
        List<String> roomNames = candidates.stream().map(PendingUserReply.RoomCandidate::roomName).toList();
        telegramMessageService.sendRoomDisambiguationAskMessage(event.chatId(), event.botType(), roomNames);
    }

}
