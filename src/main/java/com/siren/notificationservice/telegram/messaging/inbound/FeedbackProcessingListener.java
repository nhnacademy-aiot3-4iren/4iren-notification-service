package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.service.FeedbackPersistenceService;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackProcessingListener {
    private final FeedbackPersistenceService feedbackPersistenceService;

    /**
     * 강의실이 확정되고나서 받는 이벤트.
     * 예상 밖 실패(DB 등)는 삼키지 않고 던져서 재시도→DLQ로 보낸다.
     */
    @RabbitListener(queues = "#{@feedbackProcessingQueue.name}")
    public void handle(FeedbackProcessingEvent event) {

        // 피드백 시점: 사용자가 텍스트에서 언급한 시간을 기준으로 없으면 메시지 도착 시각으로 대체함 "아까 2시에 더웠어요" 2시를 기준으로해야됨
        LocalDateTime referenceAtLocal = event.experiencedAt() != null ? event.experiencedAt() : event.receivedAt();
        LocalDateTime referenceAt = referenceAtLocal;
        feedbackPersistenceService.persist(event, referenceAt);

    }
}
