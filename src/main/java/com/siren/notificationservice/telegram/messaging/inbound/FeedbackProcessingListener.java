package com.siren.notificationservice.telegram.messaging.inbound;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.request.RoomSensorsReadingRequest;
import com.siren.notificationservice.core.dto.response.OutsideWeather;
import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.FeedbackPersistenceService;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackProcessingListener {


    private final CoreApiClient coreApiClient;
    private final FeedbackPersistenceService feedbackPersistenceService;
    /**
     * 강의실이 확정되고나서 받는 이벤트
     * DLQ가 없어서 우선 try-catch로 무한 재큐잉방지함
     */
    @RabbitListener(queues = "#{@feedbackProcessingQueue.name}")
    public void handle(FeedbackProcessingEvent event) throws Exception {
        try{
            /// 피드백 시점: 사용자가 텍스트에서 언급한 시간을 기준으로 없으면 메시지 도착 시각으로 대체함 "아까 2시에 더웠어요" 2시를 기준으로해야됨
            ZonedDateTime referenceAt = event.experiencedAt() != null ? event.experiencedAt() : event.receivedAt();

            RoomEnvironmentReadingResponse readings = fetchOrNull("강의실 실측값", event.roomId(), referenceAt,
                    ()-> coreApiClient.getRoomSensorsReadings(new RoomSensorsReadingRequest(event.roomId(), referenceAt.toLocalDateTime())));
            OutsideWeather outsideWeather = fetchOrNull("외부 날씨", event.roomId(), referenceAt,
                    ()-> coreApiClient.getOutsideWeather(event.roomId(), referenceAt.toLocalDateTime()));

            feedbackPersistenceService.persist(event,referenceAt,outsideWeather,readings);
        }catch (Exception e){
            // 위 어디서든(readings 조회든 DB 저장이든) 예상 못 한 예외가 나도 여기서 끝낸다.
            // 이 로그가 이 피드백이 유실됐다는 걸 알 수 있는 유일한 흔적이라 event 전체를 남긴다.
            // TODO: DLQ를 남겨야하나 고민을해볼 여지가 있는 부분
            log.error("[FeedbackProcessingListener] 피드백 처리 실패, 데이터 유실 가능 (event={})", event, e);
        }
    }

    private <T> T fetchOrNull(String label, Long roomId, ZonedDateTime referenceAt, Supplier<T> call) {
        try{
            return call.get();
        }catch (CoreApiUnavailableException e){
            /// core에서 환경 스냅샷을 못 받아와도 피드백 자체를 버리면 안됨 따라서 로그만 찍음
            log.warn("[FeedbackProcessingListener] {} 조회 실패, 스냅샷 없이 저장 (roomId={}, referenceAt={})", label ,roomId, referenceAt, e);
            return null;
        }
    }
}
