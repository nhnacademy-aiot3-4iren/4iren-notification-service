package com.siren.notificationservice.telegram.routing.handle.impl;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.client.RecommendationApiClient;
import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.LastMentionedRoomService;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handle.IntentRouteHandler;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuestionRouteHandler implements IntentRouteHandler {
    private final RecommendationApiClient recommendationApiClient;
    private final CoreApiClient coreApiClient;
    private final LastMentionedRoomService lastMentionedRoomService;
    private final TelegramMessageService  telegramMessageService;

    @Override
    public IntentType supports() {
        return IntentType.QUESTION;
    }

    @Override
    public void handle(TelegramInboundEvent event, Long userId) {

        Long lastMentionRoomId = lastMentionedRoomService.find(userId).orElse(null);

        List<Long> subscribedRoomIds;
        try {
            subscribedRoomIds = coreApiClient.getRoomSubscriptions(userId)
                    .roomSubInfo()
                    .stream()
                    .map(UserRoomSubResponse.RoomSubResponse::roomId)
                    .toList();
        } catch (CoreApiUnavailableException e) {
            telegramMessageService.sendCoreApiUnavailableMessage(event.chatId(), event.botType());
            return;
        }

        RecommendationResponse response = recommendationApiClient.getRecommendation(userId, new RecommendationRequest(
                lastMentionRoomId, subscribedRoomIds, event.question(), event.requestAt()
        ));

        // redis에 마지막 룸 정보 캐싱
        if(response.roomId()!= null){
            lastMentionedRoomService.save(userId, response.roomId());
        }

        telegramMessageService.sendMessage(event.chatId(), event.botType(), response.answer(), "[Recommendation API] - LLM 답변");
    }
}
