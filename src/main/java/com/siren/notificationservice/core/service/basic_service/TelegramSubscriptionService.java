package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.repository.TelegramSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 발송 대상 텔레그램 연동 조회 전용 서비스. 쓰기(TelegramInboundSubService)와 역할을 분리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramSubscriptionService {

    private final TelegramSubscriptionRepository telegramSubscriptionRepository;

    /**
     * 주어진 유저들 중 Admin 봇으로 연동돼 있고 차단하지 않은 건을 조회한다. 긴급 알림 대상용.
     */
    public List<TelegramSubscription> findActiveAdminSubscriptions(List<Long> userIds) {
        return telegramSubscriptionRepository.findByUserIdInAndBotTypeAndActiveTrue(userIds, BotType.ADMIN_BOT);
    }

    /**
     * 주어진 유저들 중 봇 타입 무관하게 연동돼 있고 차단하지 않은 건 전부 조회한다. 비긴급 알림 대상용.
     */
    public List<TelegramSubscription> findActiveSubscriptions(List<Long> userIds) {
        return telegramSubscriptionRepository.findByUserIdInAndActiveTrue(userIds);
    }
}
