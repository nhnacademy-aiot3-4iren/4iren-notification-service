package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.exception.MissingChatIdException;
import com.siren.notificationservice.core.exception.TelegramSubscriptionNotFoundException;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.agent.IntentClassificationAgent;
import com.siren.notificationservice.telegram.callback.CallbackActionType;
import com.siren.notificationservice.telegram.callback.handler.CallbackRouteDispatcher;
import com.siren.notificationservice.telegram.dto.LinkTokenData;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import com.siren.notificationservice.telegram.service.TelegramInboundSubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

/**
 * 전체 흐름
 * 사용자 (텔레그램연동하기 클릭) -> DeepLinkController(딥링크 제공) 및 토큰 redis 저장 -> 사용자(딥링크 클릭 후 텔레그램으로 이동)-> 사용자 /start 클릭 ->
 * webHookController로 수신 -> RabbitMq -> TelegramInboundListener 수신 -> redis에서 토큰 비교 -> db에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramInboundListener {

    private final TelegramLinkTokenService telegramLinkTokenService;
    private final TelegramSubscriptionService telegramSubscriptionService;
    private final TelegramInboundSubService telegramInboundSubService;
    private final IntentClassificationAgent intentClassificationAgent;
    private final TelegramMessageService telegramMessageService;
    private final CallbackRouteDispatcher callbackRouteDispatcher;

    /**
     * 큐에 쌓인 텔레그램 업데이트를 update 종류별로 분기 처리한다.
     *
     * @param event botType + Telegram Update
     */
    @RabbitListener(queues = "#{@telegramInboundQueue.name}")
    public void handle(TelegramInboundEvent event) {
        Update update = event.update();

        if (update.hasMessage() && update.getMessage().hasText()
                && update.getMessage().getText().startsWith("/start")) {
            handleStartCommand(event);
        } else if (update.hasMyChatMember()) { // 봇 자신의 채팅방 소속 상태나 권한이 변경되었을때
            telegramInboundSubService.handleBlockedBot(event);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleIntentFreeText(event);
        } else if (update.hasMessage()) {
            handleUnsupportedContent(event);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(event);
        }
    }

    /**
     * "/start {token}" 메시지를 처리한다. 토큰을 검증해 userId를 얻고,
     * 이미 연동 row가 있으면 chatId/createdAt만 갱신하고, 없으면 새로 만든다.
     * 토큰이 없거나(맨 "/start"만 오는 경우, 예: 봇 차단 해제 시 텔레그램이 자동으로 재전송하는 케이스)
     * 만료/이미 소비된 경우엔 재시도해도 절대 성공할 수 없으므로 예외 없이 조용히 무시한다
     * — 재시도/DLQ로 보내도 무의미한 케이스라 의도적으로 흡수한다. 사용자는 프론트에서 토큰을 다시 발급받아야 한다.
     *
     * @param event 원본 이벤트 (botType 확인용)
     */
    private void handleStartCommand(TelegramInboundEvent event) {
        Update update = event.update();
        String token = update.getMessage().getText().substring("/start".length()).trim();
        if (token.isBlank()) {
            log.info("토큰 없는 /start 수신 (botType={}), 무시함", event.botType()); // 차단을 했다가 해제를 하면 봇이 자동으로 /start를 보내버려서 토큰 없는 값이 생김
            return;
        }

        LinkTokenData data = telegramLinkTokenService.consumeToken(token, event.botType()).orElse(null);
        if (data == null) {
            log.info("만료되었거나 이미 사용된 딥링크 토큰 수신 (botType={}), 사용자에게 안내", event.botType());
            telegramMessageService.sendTokenExpiredMessage(event.chatId(), event.botType());
            return;
        }

        telegramInboundSubService.handleValidStart(event, data); //연동 로직체크
        telegramMessageService.sendLinkSuccessMessage(event.chatId(), event.botType());// 연동 성공 메세지
    }

    /**
     * 자유 텍스트에 대해서 LLM이 의도 분류를 하고 의도에 따른 로직을 실행합니다.
     *
     * @param event
     */
    private void handleIntentFreeText(TelegramInboundEvent event) {
        String chatId = event.chatId();
        Optional<Long> userId = resolveLinkedUserId(chatId, event.botType());
        if (userId.isEmpty()) {
            return;
        }

        if (event.botType() == BotType.ADMIN_BOT) { // admin bot일 시 자유텍스트를 지원하지않음
            String deepLinkUrl = telegramLinkTokenService.getRedirectUrl(userId.get(), BotType.USER_BOT);
            telegramMessageService.sendRedirectToUserBotMessage(chatId, deepLinkUrl);
            return;
        }

        intentClassificationAgent.classify(event, userId.get());
    }

    private void handleCallbackQuery(TelegramInboundEvent event) {
        CallbackQuery callbackQuery = event.update().getCallbackQuery();
        telegramMessageService.answerCallback(callbackQuery.getId(), event.botType());

        String chatId = event.chatId();
        Optional<Long> userId = resolveLinkedUserId(chatId, event.botType());
        if (userId.isEmpty()) {
            return;
        }

        Optional<CallbackActionType> actionType = event.callbackActionType();
        if (actionType.isEmpty()) {
            log.warn("알 수 없는 콜백 형식 (chatId={}, data={})", chatId, callbackQuery.getData());
            return;
        }

        callbackRouteDispatcher.dispatch(actionType.get(), event, userId.get());
    }

    /**
     * chatId로 연동된 userId를 조회한다. 자유 텍스트/콜백 두 진입점이 공유하는 조회부 —
     * 연동 안 된 경우 안내 메시지를 보내고 빈 Optional을 반환해서, 두 곳 다 여기서
     * TelegramSubscriptionNotFoundException을 흡수한다 (사용자에게 안내로 끝나는 케이스라 재시도/DLQ 대상 아님).
     * MissingChatIdException도 같은 이유로 흡수-> chatId 자체가 없으면 안내 메시지를 보낼
     * 곳도 없으니 로그만 남기고 넘어간다. (그 외 예상 밖 예외는 안 잡고 던져서 재시도→DLQ로 감)
     */
    private Optional<Long> resolveLinkedUserId(String chatId, BotType botType) {
        try {
            return Optional.of(telegramSubscriptionService.getUserIdByChatId(chatId, botType));
        } catch (TelegramSubscriptionNotFoundException e) {
            telegramMessageService.sendNotLinkedGuideMessage(chatId, botType);
            return Optional.empty();
        } catch (MissingChatIdException e) {
            log.warn("chatId 없이 들어온 이벤트, 응답 불가 (botType={})", botType, e);
            return Optional.empty();
        }
    }

    private void handleUnsupportedContent(TelegramInboundEvent event) {
        telegramMessageService.sendUnsupportedContentMessage(event.chatId(), event.botType());
    }

}
