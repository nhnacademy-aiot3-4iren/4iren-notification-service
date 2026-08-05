package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.service.FeedbackPersistenceService;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackProcessingListener {

    // FeedbackProcessingEvent는 큐 안에서만 도는 내부 이벤트라 LocalDateTime이지만,
    // 여기서 DB로 넘어가는 경계이므로 엔티티가 쓰는 ZonedDateTime으로 변환한다.
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final FeedbackPersistenceService feedbackPersistenceService;

    /**
     * 강의실이 확정되고나서 받는 이벤트
     * DLQ가 없어서 우선 try-catch로 무한 재큐잉방지함
     */
    @RabbitListener(queues = "#{@feedbackProcessingQueue.name}")
    public void handle(FeedbackProcessingEvent event) {
        try{
            /// 피드백 시점: 사용자가 텍스트에서 언급한 시간을 기준으로 없으면 메시지 도착 시각으로 대체함 "아까 2시에 더웠어요" 2시를 기준으로해야됨
            LocalDateTime referenceAtLocal = event.experiencedAt() != null ? event.experiencedAt() : event.receivedAt();
            ZonedDateTime referenceAt = referenceAtLocal.atZone(ZONE);
            feedbackPersistenceService.persist(event,referenceAt);
        }catch (Exception e){
            // 위 어디서든(metrics 조회든 DB 저장이든) 예상 못 한 예외가 나도 여기서 끝낸다.
            // 이 로그가 이 피드백이 유실됐다는 걸 알 수 있는 유일한 흔적이라 event 전체를 남긴다.
            // TODO: DLQ를 남겨야하나 고민을해볼 여지가 있는 부분
            log.error("[FeedbackProcessingListener] 피드백 처리 실패, 데이터 유실 가능 (event={})", event, e);
        }
    }
}
