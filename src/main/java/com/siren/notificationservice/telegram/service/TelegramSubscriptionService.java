package com.siren.notificationservice.telegram.service;

import com.siren.notificationservice.core.entity.TelegramSubscription;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * TelegramInboundListener 속 DB 커넥션 문제때문에 생긴 클래스입니다.
 * handle에서 트랜잭션을 걸면 (DB쓰기 + 텔레그램 성공메세지보내기 등) 커넥션 홀딩 발생
 * 따라서 DB 쓰기 접근하는 부분의 로직을 아래 클래스로 분리하도록함
 */
@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramSubscriptionService {
    private final TelegramSubscriptionRepository telegramSubscriptionRepository;

    /**
     * 유효한 토큰으로 확인된 "/start" 요청을 실제로 반영한다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     * @param userId 토큰에 매핑된 유저 id
     */
    public void handleValidStart(TelegramInboundEvent event, Long userId) {
        Update update = event.update();
        String chatId = update.getMessage().getChatId().toString();
        ZonedDateTime now = ZonedDateTime.now();

        // 연동이 최초인지, 재연동인지 판단
        Optional<TelegramSubscription> existing =
                telegramSubscriptionRepository.findByUserIdAndBotType(userId, event.botType());

        if (existing.isPresent()) {
            existing.get().link(chatId, now);
        } else {
            TelegramSubscription subscription = TelegramSubscription.builder()
                    .userId(userId)
                    .botType(event.botType())
                    .chatId(chatId)
                    .active(true)
                    .createdAt(now)
                    .build();
            telegramSubscriptionRepository.save(subscription);
        }
    }

    /**
     * my_chat_member 웹훅을 처리한다. 유저가 봇을 차단/차단해제하면 텔레그램이 이 이벤트를 보낸다.
     * 이 이벤트엔 우리 쪽 userId가 없고 chat_id만 있어서, chat_id로 연동 row를 찾아 상태를 반영한다.
     *
     * @param event  원본 이벤트 (botType 확인용)
     */
    public void handleBlockedBot(TelegramInboundEvent event) {
        Update update = event.update();
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
