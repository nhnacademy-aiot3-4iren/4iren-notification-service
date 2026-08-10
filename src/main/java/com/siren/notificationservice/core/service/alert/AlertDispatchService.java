package com.siren.notificationservice.core.service.alert;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.AlertHistoryKey;
import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.dto.response.RoomSubscribersResponse;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.service.basic_service.AlertHistoryService;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AlertEvent를 실제 수신자에게 발송하고 이력을 남긴다. Core 조회 + 텔레그램 발송(둘 다 블로킹 외부 호출)을
 * DB 트랜잭션 밖에서 수행한다
 * 의도적으로 @Transactional 없음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertDispatchService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final CoreApiClient coreApiClient;
    private final TelegramSubscriptionService telegramSubscriptionService;
    private final AlertMessageFormatter alertMessageFormatter;
    private final TelegramMessageService telegramMessageService;
    private final AlertHistoryService alertHistoryService;
    private final AlertDigestBufferService alertDigestBufferService;

    /**
     * 긴급 알림 - 해당 룸 관리자 전원에게 즉시 개별 발송한다.
     */
    public void dispatchUrgent(AlertEvent event) {
        RoomSubscribersResponse response = coreApiClient.getSubscribers(event.roomId()); //해당 룸 아이디를 가진 사용자 조회
        List<Long> recipientUserIds = extractRecipients(response);
        List<TelegramSubscription> subscriptions =
                telegramSubscriptionService.findActiveAdminSubscriptions(recipientUserIds);
        sendAndRecord(event, response.roomName(), subscriptions);
    }

    /**
     * 비긴급 알림 - notificationEnabled인 구독자 + 관리자 전원, 연동된 봇 채널 전부로 발송한다.
     * 다이제스트 버퍼에 3분간 쌓아두고 전송
     */
    public void dispatchDigest(AlertEvent event) {
        RoomSubscribersResponse response = coreApiClient.getSubscribers(event.roomId());
        List<Long> recipientUserIds = extractRecipients(response);
        AlertDigestBufferEntry entry = new AlertDigestBufferEntry(event, response.roomName());
        recipientUserIds.forEach(userId-> alertDigestBufferService.buffer(userId, entry));
    }

    /**
     * Core 응답에서 발송 대상 userId만 뽑는다 알림 수신 여부(notificationEnabled)에 따른 userId만 추출한다.
     */
    private List<Long> extractRecipients(RoomSubscribersResponse response) {
        return response.subscribers().stream()
                .filter(RoomSubscribersResponse.SubscribersResponse::notificationEnabled)
                .map(RoomSubscribersResponse.SubscribersResponse::userId)
                .toList();
    }

    /**
     * 긴급용 / 비긴급은 Listener에서 처리
     * 대상마다 (eventId, userId, botType) 중복 발송 여부를 확인하고, 아니면 발송 후 이력을 남긴다.
     * exists 확인과 save 사이 경합 가능성은 (event_id, user_id, bot_type) UNIQUE 제약이 최종 방어선.
     */
    private void sendAndRecord(AlertEvent event, String roomName, List<TelegramSubscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            log.info("[AlertDispatchService] 발송 대상 없음 (eventId={}, roomId={})", event.eventId(), event.roomId());
            return;
        }
        String message = alertMessageFormatter.format(event, roomName);
        List<AlertHistory> alertHistories = new ArrayList<>();

        // 해당 이벤트에 대해 이미 발송된적이 있는 지 조사
        Set<AlertHistoryKey> keys = alertHistoryService.findAlreadySentKeys(event.eventId());

        // 해당 이벤트에 대해 보낸적이 없는 TelegramSubscription만 필터링함
        List<TelegramSubscription> filteredSubs = subscriptions.stream()
                .filter(sub -> !keys.contains(new AlertHistoryKey(sub.getUserId(), sub.getBotType())))
                .toList();

        for (TelegramSubscription subscription : filteredSubs) {
            telegramMessageService.sendMessage(subscription.getChatId(), subscription.getBotType(), message, "알림 발송");

            alertHistories.add(AlertHistory.builder()
                    .roomId(event.roomId())
                    .botType(subscription.getBotType())
                    .alertType(event.alertType())
                    .eventId(event.eventId())
                    .message(message)
                    .sendAt(ZonedDateTime.now(SERVICE_ZONE))
                    .userId(subscription.getUserId())
                    .build());
        }
        alertHistoryService.saveAll(alertHistories);
    }
}
