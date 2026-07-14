package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.BotType;
import com.siren.notificationservice.core.entity.TelegramSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}
