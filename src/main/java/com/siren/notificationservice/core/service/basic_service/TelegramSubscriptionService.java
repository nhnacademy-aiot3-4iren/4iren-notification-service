package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.exception.InvalidAccountRoleEventException;
import com.siren.notificationservice.core.exception.MissingChatIdException;
import com.siren.notificationservice.core.exception.TelegramSubscriptionNotFoundException;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 발송 대상 텔레그램 연동 조회 전용 서비스. 쓰기(TelegramInboundSubService)와 역할을 분리한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramSubscriptionService {
    private static final String USER_IDS_NULL_MESSAGE = "userIds는 null일 수 없습니다.";
    private static final String USER_ID_NULL_MESSAGE = "userId는 null일 수 없습니다.";
    private static final String BOT_TYPE_NULL_MESSAGE = "botType은 null일 수 없습니다.";

    private final TelegramSubscriptionRepository telegramSubscriptionRepository;

    /**
     * 주어진 유저들 중 Admin 봇으로 연동돼 있고 차단하지 않은 건을 조회한다. 긴급 알림 대상용.
     */
    public List<TelegramSubscription> findActiveAdminSubscriptions(List<Long> userIds) {
        Objects.requireNonNull(userIds, USER_IDS_NULL_MESSAGE);
        return telegramSubscriptionRepository.findByUserIdInAndBotTypeAndActiveTrue(userIds, BotType.ADMIN_BOT);
    }

    public UserRole getUserRole(Long userId){
        Objects.requireNonNull(userId, USER_ID_NULL_MESSAGE);
        return telegramSubscriptionRepository.findByUserId(userId).stream()
                .map(TelegramSubscription::getUserRole)
                .findFirst().orElse(UserRole.NORMAL);
    }

    /**
     * userId/role이 없거나 role 값을 모르면 재시도해도 결과가 달라지지 않으므로 삼키지 않고 던진다
     * — 리스너까지 전파돼 재시도 소진 후 DLQ로 가서 관리자가 직접 확인할 수 있게 한다.
     */
    @Transactional
    public void updateUserRole(Long userId, String role, LocalDateTime roleUpdateAt) {
        if (userId == null || role == null) {
            throw new InvalidAccountRoleEventException(
                    "불완전한 role 이벤트 (userId=" + userId + ", role=" + role + ")");
        }
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new InvalidAccountRoleEventException("알 수 없는 role: " + role, e);
        }
        List<TelegramSubscription> telegramSubscriptions = telegramSubscriptionRepository.findByUserId(userId);
        telegramSubscriptions.forEach(telegramSubscription
                -> telegramSubscription.updateUserRole(userRole, roleUpdateAt));
    }

    /**
     * 주어진 유저들 중 봇 타입 무관하게 연동돼 있고 차단하지 않은 건 전부 조회한다. 비긴급 알림 대상용.
     */
    public List<TelegramSubscription> findActiveSubscriptions(List<Long> userIds) {
        Objects.requireNonNull(userIds, USER_IDS_NULL_MESSAGE);
        return telegramSubscriptionRepository.findByUserIdInAndActiveTrue(userIds);
    }

    /**
     * 특정 유저가 특정 봇에 이미 연동돼 있는지 확인한다.
     * 프론트가 딥링크 토큰 발급 전 "이미 연동되어 있습니다, 재연동하시겠어요?" 확인
     * 다이얼로그를 보여줄지 판단하는 데 쓴다.
     *
     * @param userId  대상 유저 id
     * @param botType ADMIN_BOT 또는 USER_BOT
     * @return 연동 여부
     */
    public boolean isLinked(Long userId, BotType botType) {
        Objects.requireNonNull(userId, USER_ID_NULL_MESSAGE);
        Objects.requireNonNull(botType, BOT_TYPE_NULL_MESSAGE);
        return telegramSubscriptionRepository.existsByUserIdAndBotType(userId, botType);
    }

    /**
     * chatId와 botType으로 연동된 유저 id를 조회한다.
     *
     * @param chatId  대상 텔레그램 chat_id
     * @param botType ADMIN_BOT 또는 USER_BOT
     * @return 연동된 유저 id
     */
    public Long getUserIdByChatId(String chatId, BotType botType) {
        if(chatId == null){
            throw new MissingChatIdException();
        }
        Objects.requireNonNull(botType, BOT_TYPE_NULL_MESSAGE);
        return telegramSubscriptionRepository.findByChatIdAndBotType(chatId, botType)
                .map(TelegramSubscription::getUserId)
                .orElseThrow(TelegramSubscriptionNotFoundException::new);
    }

    public List<TelegramSubscription> getTelegramSubscriptions(Long userId) {
        Objects.requireNonNull(userId, USER_ID_NULL_MESSAGE);
        return telegramSubscriptionRepository.findByUserId(userId);
    }
}
