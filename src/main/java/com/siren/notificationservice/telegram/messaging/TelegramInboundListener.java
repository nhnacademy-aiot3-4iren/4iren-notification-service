package com.siren.notificationservice.telegram.messaging;

import com.siren.notificationservice.core.entity.TelegramSubscription;
import com.siren.notificationservice.core.repository.NotificationUserRepository;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.exception.TokenExpiredException;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;

import java.time.ZonedDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TelegramInboundListener {

    private final TelegramLinkTokenService telegramLinkTokenService;
    private final TelegramSubscriptionRepository telegramSubscriptionRepository;
    private final NotificationUserRepository notificationUserRepository;

    /**
     * 큐에 쌓인 텔레그램 업데이트를 update 종류별로 분기 처리한다.
     *
     * @param event botType + Telegram Update
     */
    @RabbitListener(queues = "#{@telegramInboundQueue.name}")
    @Transactional
    public void handle(TelegramInboundEvent event) {
        Update update = event.update();

        if (update.hasMessage() && update.getMessage().hasText()
                && update.getMessage().getText().startsWith("/start")) {
            handleStartCommand(event, update);
        } else if (update.hasMyChatMember()) { // 봇 자신의 채팅방 소속 상태나 권한이 변경되었을때
            handleBlockedBot(event, update);
        }
        // 그 외 자연어 메시지는 Phase 5 의도분류에서 처리
    }

    /**
     * "/start {token}" 메시지를 처리한다. 토큰을 검증해 userId를 얻고,
     * 이미 연동 row가 있으면 chatId/linkedAt만 갱신하고, 없으면 새로 만든다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     * @param update /start 메시지가 담긴 Update
     */
    private void handleStartCommand(TelegramInboundEvent event, Update update) {
        String token = update.getMessage().getText().substring("/start".length()).trim();
        Long userId = telegramLinkTokenService.consumeToken(token, event.botType())
                .orElseThrow(() -> new TokenExpiredException("DeepLink: UUID token expired"));
        String chatId = update.getMessage().getChatId().toString();
        ZonedDateTime linkedAt = ZonedDateTime.now();

        // 연동이 최초인지, 재연동인지 판단
        Optional<TelegramSubscription> existing =
                telegramSubscriptionRepository.findByNotificationUser_UserIdAndBotType(userId, event.botType());

        if (existing.isPresent()) {
            existing.get().link(chatId, linkedAt);
        } else {
            TelegramSubscription subscription = TelegramSubscription.builder()
                    .notificationUser(notificationUserRepository.getReferenceById(userId)) //프록시로 처리
                    .botType(event.botType())
                    .chatId(chatId)
                    .active(true)
                    .linkedAt(linkedAt)
                    .build();
            telegramSubscriptionRepository.save(subscription);
        }
    }

    /**
     * my_chat_member 웹훅을 처리한다. 유저가 봇을 차단/차단해제하면 텔레그램이 이 이벤트를 보낸다.
     * 이 이벤트엔 우리 쪽 userId가 없고 chat_id만 있어서, chat_id로 연동 row를 찾아 상태를 반영한다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     * @param update my_chat_member 정보가 담긴 Update
     */
    private void handleBlockedBot(TelegramInboundEvent event, Update update) {
        ChatMemberUpdated chatMemberUpdated = update.getMyChatMember();
        String newStatus = chatMemberUpdated.getNewChatMember().getStatus(); // 새로운 상태의 status 값을 확인
        String chatId = chatMemberUpdated.getChat().getId().toString();
        Optional<TelegramSubscription> telegramSubscription =
                telegramSubscriptionRepository.findByChatIdAndBotType(chatId, event.botType());

        if (ChatMemberBanned.STATUS.equals(newStatus)) { //사용자가 봇 차단
            telegramSubscription.ifPresent(TelegramSubscription::block);
        } else if (ChatMemberMember.STATUS.equals(newStatus)) { //사용자가 봇 차단 해제
            telegramSubscription.ifPresent(TelegramSubscription::unblock);
        }
    }
}
