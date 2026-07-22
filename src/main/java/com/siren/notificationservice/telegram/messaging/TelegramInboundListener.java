package com.siren.notificationservice.telegram.messaging;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.exception.TelegramSubscriptionNotFoundException;
import com.siren.notificationservice.telegram.agent.IntentClassificationAgent;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import com.siren.notificationservice.telegram.service.TelegramSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
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
    private final IntentClassificationAgent intentClassificationAgent;
    private final TelegramMessageService telegramMessageService;

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
            telegramSubscriptionService.handleBlockedBot(event);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleIntentFreeText(event);
        }else if(update.hasMessage()) {
            handleUnsupportedContent(event);
        }
        // 그 외(callback_query 등 메시지 자체가 없는 업데이트)는 여전히 무시
    }

    /**
     * "/start {token}" 메시지를 처리한다. 토큰을 검증해 userId를 얻고,
     * 이미 연동 row가 있으면 chatId/createdAt만 갱신하고, 없으면 새로 만든다.
     * 토큰이 없거나(맨 "/start"만 오는 경우, 예: 봇 차단 해제 시 텔레그램이 자동으로 재전송하는 케이스)
     * 만료/이미 소비된 경우엔 재시도해도 절대 성공할 수 없으므로 예외 없이 조용히 무시한다
     * — 그냥 던지면 DLQ가 없어서 무한 재큐잉된다. 사용자는 프론트에서 토큰을 다시 발급받아야 한다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     */
    private void handleStartCommand(TelegramInboundEvent event) {
        Update update = event.update();
        String token = update.getMessage().getText().substring("/start".length()).trim();
        if (token.isBlank()) {
            log.info("토큰 없는 /start 수신 (botType={}), 무시함", event.botType()); // 차단을 했다가 해제를 하면 봇이 자동으로 /start를 보내버려서 토큰 없는 값이 생김
            return;
        }

        Optional<Long> userId = telegramLinkTokenService.consumeToken(token, event.botType());
        if (userId.isEmpty()) {
            log.info("만료되었거나 이미 사용된 딥링크 토큰 수신 (botType={}), 사용자에게 안내", event.botType());
            handleExpiredToken(event);
            return;
        }

        telegramSubscriptionService.handleValidStart(event, userId.get()); //연동 로직체크
        handleSuccessLink(event); // 연동 성공 메세지
    }

    /**
     * 만료됐거나 이미 소비된 토큰으로 "/start"가 왔을 때, 다시 토큰을 발급받으라고 안내한다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     */
    private void handleExpiredToken(TelegramInboundEvent event) {
        telegramMessageService.sendTokenExpiredMessage(event.chatId(), event.botType());
    }

    /**
     * 연동이 성공적으로 끝났음을 사용자에게 텔레그램 챗으로 알려준다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     */
    private void handleSuccessLink(TelegramInboundEvent event){
        telegramMessageService.sendLinkSuccessMessage(event.chatId(), event.botType());
    }

    /**
     * 자유 텍스트에 대해서 LLM이 의도 분류를 하고 의도에 따른 로직을 실행합니다.
     * @param event
     */
    private void handleIntentFreeText(TelegramInboundEvent event) {
        String chatId = event.chatId();
        Long userId;
        try {
            userId = telegramLinkTokenService.getUserIdByChatId(chatId, event.botType());
        } catch (TelegramSubscriptionNotFoundException e) {
            telegramMessageService.sendNotLinkedGuideMessage(chatId, event.botType());
            return;
        }

        if(event.botType() == BotType.ADMIN_BOT){
            String deepLinkUrl = telegramLinkTokenService.getRedirectUrl(userId, BotType.USER_BOT);
            telegramMessageService.sendRedirectToUserBotMessage(chatId, deepLinkUrl);
            return;
        }

        intentClassificationAgent.classify(event, userId);
    }

    private void handleUnsupportedContent(TelegramInboundEvent event) {
        telegramMessageService.sendUnsupportedContentMessage(event.chatId(), event.botType());
    }

}
