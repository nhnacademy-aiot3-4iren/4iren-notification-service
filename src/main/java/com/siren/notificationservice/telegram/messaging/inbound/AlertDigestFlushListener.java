package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.dto.AlertHistoryKey;
import com.siren.notificationservice.core.dto.event.AlertDigestBufferEntry;
import com.siren.notificationservice.core.dto.event.AlertDigestFlushMessage;
import com.siren.notificationservice.core.dto.event.AlertEvent;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import com.siren.notificationservice.core.entity.table.TelegramSubscription;
import com.siren.notificationservice.core.service.alert.AlertDigestBufferService;
import com.siren.notificationservice.core.service.alert.AlertMessageFormatter;
import com.siren.notificationservice.core.service.basic_service.AlertHistoryService;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertDigestFlushListener {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final AlertDigestBufferService alertDigestBufferService;
    private final TelegramSubscriptionService telegramSubscriptionService;
    private final AlertMessageFormatter alertMessageFormatter;
    private final TelegramMessageService telegramMessageService;
    private final AlertHistoryService alertHistoryService;

    // 이 유저 3분 지나서 메세지 발송하는 메서드
    @RabbitListener(queues = "#{@alertDigestFlushQueue.name}")
    public void flush(AlertDigestFlushMessage event) {

        Long userId = event.userId(); // 유저추출

        List<AlertDigestBufferEntry> entries = alertDigestBufferService.drain(userId); // redis에 저장됐던 것들 정리하고 추출
        if (entries.isEmpty()) return; // 비어있음 -> 종료 (메시지 안보냄)

        List<AlertDigestBufferEntry> distinct = duplicationDelByEventId(entries); // 이벤트 아이디 중복제거
        List<TelegramSubscription> subscriptions =
                telegramSubscriptionService.findActiveSubscriptions(List.of(userId)); // 해당 유저가 구독한 봇 정보 (2개 있을 수 있으니)
        if (subscriptions.isEmpty()) return; // 없으면 종료

        String digestMessage = alertMessageFormatter.formatDigest(distinct); // 하나로 합침 (방 별로 그룹핑해서)

        sendAndRecordDigest(subscriptions, distinct, digestMessage);// 전송

    }

    private List<AlertDigestBufferEntry> duplicationDelByEventId(List<AlertDigestBufferEntry> entries) {
        Set<String> seen = new HashSet<>();
        return entries.stream()
                .filter(entry -> seen.add(entry.event().eventId()))
                .toList();
    }

    private void sendAndRecordDigest(List<TelegramSubscription> subscriptions,
                                     List<AlertDigestBufferEntry> entries,
                                     String digestMessage) {
        for (TelegramSubscription subscription : subscriptions) {
            telegramMessageService.sendMessage(subscription.getChatId(),
                    subscription.getBotType(),
                    digestMessage,
                    "비긴급 알림 발송");

        }

        // alert history 저장로직
        List<AlertHistory> histories = new ArrayList<>();

        for (AlertDigestBufferEntry entry : entries) {
            AlertEvent event = entry.event();

            Set<AlertHistoryKey> alreadySent = alertHistoryService.findAlreadySentKeys(event.eventId());
            String eventSummary = alertMessageFormatter.format(event, entry.roomName());

            for (TelegramSubscription s : subscriptions) {
                if (alreadySent.contains(new AlertHistoryKey(s.getUserId(), s.getBotType()))) continue;

                histories.add(AlertHistory.builder()
                        .roomId(event.roomId())
                        .botType(s.getBotType())
                        .alertType(event.alertType())
                        .eventId(event.eventId())
                        .message(eventSummary)
                        .sendAt(ZonedDateTime.now(SERVICE_ZONE))
                        .userId(s.getUserId())
                        .build());
            }
        }
        alertHistoryService.saveAll(histories);
    }
}
