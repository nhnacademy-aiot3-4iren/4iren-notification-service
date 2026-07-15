package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.BotType;
import com.siren.notificationservice.core.entity.TelegramSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelegramSubscriptionRepository extends JpaRepository<TelegramSubscription, Long> {

    /**
     * 주어진 유저 id 목록 중 지정된 봇 타입으로 연동돼 있고 아직 차단하지 않은(active) 건만 조회한다.
     * 발송 대상 chat_id를 뽑아낼 때 쓴다.
     *
     * @param userIds 대상 유저 id 목록
     * @param botType ADMIN_BOT 또는 USER_BOT
     * @return 활성 상태인 텔레그램 연동 목록
     */
    List<TelegramSubscription> findByNotificationUser_UserIdInAndBotTypeAndActiveTrue(List<Long> userIds, BotType botType);

    /**
     * 특정 유저의 특정 봇 연동 row 하나를 조회한다 ({UNIQUE(user_id, bot_type)} 기준 단건).
     * /start 처리 시 최초 연동인지 재연동인지 판단하는 데 쓴다.
     *
     * @param userId  대상 유저 id
     * @param botType ADMIN_BOT 또는 USER_BOT
     * @return 연동 row, 없으면 empty
     */
    Optional<TelegramSubscription> findByNotificationUser_UserIdAndBotType(Long userId, BotType botType);

    /**
     * chat_id로 연동 row를 조회한다. my_chat_member 웹훅(#61)은 유저 id가 아니라
     * 텔레그램 chat_id만 알려주기 때문에, 봇 차단/차단해제 처리 시 이 메서드로 대상을 찾는다.
     *
     * @param chatId  대상 텔레그램 chat_id
     * @param botType ADMIN_BOT 또는 USER_BOT
     * @return 연동 row, 없으면 empty
     */
    Optional<TelegramSubscription> findByChatIdAndBotType(String chatId, BotType botType);
}
