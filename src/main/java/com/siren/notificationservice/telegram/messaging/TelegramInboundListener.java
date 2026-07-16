package com.siren.notificationservice.telegram.messaging;

import com.siren.notificationservice.core.entity.BotType;
import com.siren.notificationservice.telegram.config.TelegramSender;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import com.siren.notificationservice.telegram.service.TelegramSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

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
    private final TelegramSender adminTelegramSender;
    private final TelegramSender memberTelegramSender;

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
            handleStartCommand(event, update);
        } else if (update.hasMyChatMember()) { // 봇 자신의 채팅방 소속 상태나 권한이 변경되었을때
            telegramSubscriptionService.handleBlockedBot(event, update);
        }
        // 그 외 자연어 메시지는 Phase 5 의도분류에서 처리
    }

    /**
     * "/start {token}" 메시지를 처리한다. 토큰을 검증해 userId를 얻고,
     * 이미 연동 row가 있으면 chatId/linkedAt만 갱신하고, 없으면 새로 만든다.
     * 토큰이 없거나(맨 "/start"만 오는 경우, 예: 봇 차단 해제 시 텔레그램이 자동으로 재전송하는 케이스)
     * 만료/이미 소비된 경우엔 재시도해도 절대 성공할 수 없으므로 예외 없이 조용히 무시한다
     * — 그냥 던지면 DLQ가 없어서 무한 재큐잉된다. 사용자는 프론트에서 토큰을 다시 발급받아야 한다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     * @param update /start 메시지가 담긴 Update
     */
    private void handleStartCommand(TelegramInboundEvent event, Update update) {
        String token = update.getMessage().getText().substring("/start".length()).trim();
        if (token.isBlank()) {
            log.info("토큰 없는 /start 수신 (botType={}), 무시함", event.botType()); // 차단을 했다가 해제를 하면 봇이 자동으로 /start를 보내버려서 토큰 없는 값이 생김
            return;
        }

        Optional<Long> userId = telegramLinkTokenService.consumeToken(token, event.botType());
        if (userId.isEmpty()) {
            log.info("만료되었거나 이미 사용된 딥링크 토큰 수신 (botType={}), 사용자에게 안내", event.botType());
            handleExpiredToken(event,update);
            return;
        }

        telegramSubscriptionService.handleValidStart(event, update, userId.get()); //연동 로직체크
        handleSuccessLink(event, update); // 연동 성공 메세지
    }

    /**
     * 만료됐거나 이미 소비된 토큰으로 "/start"가 왔을 때, 다시 토큰을 발급받으라고 안내한다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     * @param update /start 메시지가 담긴 Update (chatId 추출용)
     */
    private void handleExpiredToken(TelegramInboundEvent event, Update update) {
        String chatId = update.getMessage().getChatId().toString();
        SendMessage message = new SendMessage(chatId, "링크가 만료되었거나 이미 사용되었습니다. 앱에서 텔레그램 연동을 다시 요청해 주세요");
        sendMessage(event,message,"링크 만료 안내");
    }

    /**
     * 연동이 성공적으로 끝났음을 사용자에게 텔레그램 챗으로 알려준다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     * @param update /start 메시지가 담긴 Update (chatId 추출용)
     */
    private void handleSuccessLink(TelegramInboundEvent event, Update update){
        String chatId = update.getMessage().getChatId().toString();
        SendMessage message = new SendMessage(chatId, "환영합니다! 성공적으로 연동되었습니다.");
        sendMessage(event,message,"연동 성공 안내");
    }

    /**
     * 실제로 텔레그램에 메시지를 발송한다. 발송 실패는 예외를 삼키고 로그만 남긴다 (재시도 없음).
     *
     * @param event   원본 이벤트 (botType으로 어느 봇 sender를 쓸지 결정)
     * @param message 보낼 메시지
     * @param context 로그 구분용 라벨 (예: "링크 만료 안내", "연동 성공 안내")
     */
    private void sendMessage(TelegramInboundEvent event, SendMessage message, String context) {
        try{
            resolveTelegramSender(event.botType()).execute(message);
        } catch (TelegramApiRequestException e) {
            if (Integer.valueOf(403).equals(e.getErrorCode())) {
                log.info("{} 발송 불가 - 봇 차단 상태 (botType={}, chatId={})", context,event.botType(), message.getChatId());
            } else {
                log.warn("{} 발송 실패 (botType={}, chatId={}, errorCode={})",
                        context,event.botType(), message.getChatId(), e.getErrorCode(), e);
            }
        } catch (TelegramApiException e) {
            log.warn("{} 발송 실패 (botType={}, chatId={})", context,event.botType(), message.getChatId(), e);
        }
    }

    /**
     * 봇에 따른 텔레그램 sender 선택
     * @param botType Admin 봇인지 User 봇인지
     * @return memberTelegramSender / adminTelegramSender
     */
    private TelegramSender resolveTelegramSender(BotType botType) {
        return switch (botType) {
            case USER_BOT -> memberTelegramSender;
            case ADMIN_BOT -> adminTelegramSender;
        };
    }
}
