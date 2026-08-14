package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TelegramSubscriptionRepository extends JpaRepository<TelegramSubscription, Long> {

    /**
     * 주어진 유저 id 목록 중 지정된 봇 타입으로 연동돼 있고 아직 차단하지 않은(active) 건만 조회한다.
     * 발송 대상 chat_id를 뽑아낼 때 쓴다.
     *
     */
    List<TelegramSubscription> findByUserIdInAndBotTypeAndActiveTrue(List<Long> userIds, BotType botType);

    /**
     * 주어진 유저 id 목록 중 봇 타입 무관하게 연동돼 있고 아직 차단하지 않은(active) 건 전부 조회한다.
     * Admin/Member 봇 둘 다 연동된 유저면 둘 다 나온다
     */
    List<TelegramSubscription> findByUserIdInAndActiveTrue(List<Long> userIds);

    /**
     * 특정 유저의 특정 봇 연동 row 하나를 조회한다 ({UNIQUE(user_id, bot_type)} 기준 단건).
     * /start 처리 시 최초 연동인지 재연동인지 판단하는 데 쓴다.
     *
     */
    Optional<TelegramSubscription> findByUserIdAndBotType(Long userId, BotType botType);

    /**
     * chat_id로 연동 row를 조회한다. my_chat_member 웹훅(#61)은 유저 id가 아니라
     * 텔레그램 chat_id만 알려주기 때문에, 봇 차단/차단해제 처리 시 이 메서드로 대상을 찾는다.
     *
     */
    Optional<TelegramSubscription> findByChatIdAndBotType(String chatId, BotType botType);

    /**
     * 특정 유저가 특정 봇에 이미 연동돼 있는지 확인한다. 딥링크 토큰 발급 전
     * 프론트가 재연동 확인 다이얼로그를 보여줄지 판단하는 데 쓴다 (#67).
     */
    boolean existsByUserIdAndBotType(Long userId, BotType botType);

    List<TelegramSubscription> findByUserId(Long userId);

    @Query("SELECT s.botType FROM TelegramSubscription s WHERE s.userId=:userId AND s.active = true")
    List<BotType> findActiveBotTypesByUserId(Long userId);


}
