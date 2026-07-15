package com.siren.notificationservice.core.messaging;

import com.siren.notificationservice.core.dto.event.UserSyncEvent;
import com.siren.notificationservice.core.entity.NotificationUser;
import com.siren.notificationservice.core.repository.NotificationUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Account 에서 유저 정보가 업데이트 될 때
 * Notification API에서 Account와의 동기화를 위해 생성
 */
@Component
@RequiredArgsConstructor
public class UserSyncEventListener {


    private final NotificationUserRepository notificationUserRepository;

    @RabbitListener(queues = "#{@userSyncQueue.name}")
    @Transactional
    public void handle(UserSyncEvent userSyncEvent) {
        Optional<NotificationUser> notificationUser = notificationUserRepository.findById(userSyncEvent.userId());

        if (notificationUser.isPresent()) {
            notificationUser.get().syncFrom(userSyncEvent.userName(),
                    userSyncEvent.active(),
                    userSyncEvent.syncedAt());
        }else{
            NotificationUser newNotificationUser = NotificationUser.builder()
                    .userId(userSyncEvent.userId())
                    .userName(userSyncEvent.userName())
                    .active(userSyncEvent.active())
                    .syncedAt(userSyncEvent.syncedAt())
                    .build();
            notificationUserRepository.save(newNotificationUser);
        }
    }
}
