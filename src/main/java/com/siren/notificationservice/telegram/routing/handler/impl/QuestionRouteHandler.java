package com.siren.notificationservice.telegram.routing.handler.impl;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.client.RecommendationApiClient;
import com.siren.notificationservice.core.dto.ConversationContext;
import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import com.siren.notificationservice.core.dto.response.RoomSubResponse;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.core.service.cache.LastMentionedRoomService;
import com.siren.notificationservice.core.service.cache.LlmConversationContextService;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handler.IntentRouteHandler;
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
    private final TelegramSubscriptionService telegramSubscriptionService;
    private final CoreApiClient coreApiClient;
    private final LastMentionedRoomService lastMentionedRoomService;
    private final TelegramMessageService  telegramMessageService;
    private final LlmConversationContextService llmConversationContextService;

    @Override
    public IntentType supports() {
        return IntentType.QUESTION;
    }


    @Override
    public void handle(TelegramInboundEvent event, Long userId) {
        List<RoomSubResponse> roomSubInfo;
        try {
            // core API쪽에 해당 유저가 구독한 룸들의 정보를 조회요청함
            roomSubInfo = coreApiClient.getRoomSubscriptions(userId)
                    .roomSubInfo()
                    .stream()
                    .toList();
        } catch (CoreApiUnavailableException e) {
            // core API fallback으로 CoreApiUnavailableException이 터지면
            // 지금은 확인이 어려워요, 잠시 후 다시 시도해주세요. 라고 유저에게 메시지 보냄
            telegramMessageService.sendCoreApiUnavailableMessage(event.chatId(), event.botType());
            return;
        }

        UserRole role = telegramSubscriptionService.getUserRole(userId);
        // recommendation API 부분에 요청을 보내고 받음
        RecommendationResponse response = recommendationApiClient.getRecommendation(userId, role, "TELEGRAM", new RecommendationRequest(
                roomSubInfo, event.question(), event.requestAt()
        ));

        // redis에 마지막 룸 정보 캐싱
        if(response.roomId()!= null){
            lastMentionedRoomService.save(userId, response.roomId());
        }

        if(response.answer().options()!=null && !response.answer().options().isEmpty()) {
            telegramMessageService.sendInlineKeyboardMessage(event.chatId(), event.botType(), response.answer().answer(), CallbackActionType.QUESTION_CONTINUE, response.answer().options());
        }else{
            llmConversationContextService.save(userId, new ConversationContext(
                    IntentType.QUESTION.name(), event.question(), response.answer().answer()));
            telegramMessageService.sendMessage(event.chatId(), event.botType(),response.answer().answer(),"[Recommendation API] - LLM 답변");
        }
    }
}
