package com.siren.notificationservice.core.messaging;

import com.siren.notificationservice.core.dto.event.SubscriberSyncEvent;
import com.siren.notificationservice.core.entity.NotificationUser;
import com.siren.notificationservice.core.entity.RoomSubscription;
import com.siren.notificationservice.core.entity.RoomSubscriptionId;
import com.siren.notificationservice.core.exception.NotificationUserNotFoundException;
import com.siren.notificationservice.core.repository.NotificationUserRepository;
import com.siren.notificationservice.core.repository.RoomSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoomSubscriptionListener {

    private final RoomSubscriptionRepository roomSubscriptionRepository;
    private final NotificationUserRepository notificationUserRepository;

    /**
     * Web이 발행한 구독 상태 스냅샷(상태 + 알림 수신 여부)을 반영한다.
     * 이미 구독 row가 있으면 RoomSubscription.syncFrom으로 갱신하고,
     * 없으면(최초 승인) 새로 만들어 저장한다.
     *
     * @param event 구독 상태 스냅샷 이벤트
     */
    @RabbitListener(queues = "#{@subscribeSyncQueue.name}")
    @Transactional
    public void handle(SubscriberSyncEvent event) {
        RoomSubscriptionId id = RoomSubscriptionId.builder()
                .userId(event.userId())
                .roomId(event.roomId())
                .build();

        Optional<RoomSubscription> roomSubscription = roomSubscriptionRepository.findById(id);

        if (roomSubscription.isPresent()) {
            roomSubscription.get().syncFrom(event.status(), event.alarmEnabled(), event.updatedAt());
        } else {
            RoomSubscription newSubscription = RoomSubscription.builder()
                    .id(id)
                    .notificationUser(resolveNotificationUser(event.userId(), event.updatedAt()))
                    .status(event.status())
                    .alarmEnable(event.alarmEnabled())
                    .updatedAt(event.updatedAt())
                    .build();
            roomSubscriptionRepository.save(newSubscription);
        }
    }

    /**
     * userId로 NotificationUser를 조회하고, 없으면 최소 정보(이름 미상, 활성)로 생성한다.
     * subscribeSyncQueue가 userSyncQueue보다 먼저 처리되는 이벤트 순서 역전에 대비한 것으로,
     * 나중에 실제 UserSyncEvent가 도착하면 NotificationUser.syncFrom이 이름/활성상태를 채워준다.
     * 같은 신규 유저에 대한 이벤트 두 개가 동시에 처리되면 생성 시점에 PK가 경합할 수 있어,
     * 그 경우 한 번 더 조회해서 먼저 커밋된 row를 사용한다.
     *
     * @param userId         대상 유저 id
     * @param eventUpdatedAt 최소 정보 생성 시 synced_at으로 쓸 이벤트 시각
     * @return 기존 또는 새로 생성된 NotificationUser
     */
    private NotificationUser resolveNotificationUser(Long userId, ZonedDateTime eventUpdatedAt) {
        return notificationUserRepository.findById(userId)
                .orElseGet(() -> createPlaceholder(userId, eventUpdatedAt));
    }

    private NotificationUser createPlaceholder(Long userId, ZonedDateTime eventUpdatedAt) {
        try {
            NotificationUser placeholder = NotificationUser.builder()
                    .userId(userId)
                    .userName("")
                    .active(true)
                    .syncedAt(eventUpdatedAt)
                    .build();
            return notificationUserRepository.saveAndFlush(placeholder);
        } catch (DataIntegrityViolationException e) {
            return notificationUserRepository.findById(userId)
                    .orElseThrow(() -> new NotificationUserNotFoundException(userId));
        }
    }
}
