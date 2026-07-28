package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.repository.FeedbackLogRepository;
import com.siren.notificationservice.core.repository.FeedbackScoreRepository;
import com.siren.notificationservice.core.repository.OutsideWeatherSnapshotRepository;
import com.siren.notificationservice.core.repository.RoomEnvironmentReadingRepository;
import com.siren.notificationservice.core.repository.RoomEnvironmentSnapshotRepository;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * FeedbackProcessingEvent를 실제 DB 레코드(FeedbackLog/FeedbackScore/
 * RoomEnvironmentSnapshot·Reading/OutsideWeatherSnapshot)로 저장한다. Core API 호출이
 * 끝난 뒤에만 호출되도록 설계되어 있다 — 이 클래스 안엔 블로킹 외부 호출이 전혀 없어서
 * @Transactional 범위 안에 있어도 커넥션을 오래 붙잡을 일이 없다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackPersistenceService {
    private final RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository;
    private final RoomEnvironmentReadingRepository roomEnvironmentReadingRepository;
    private final OutsideWeatherSnapshotRepository outsideWeatherSnapshotRepository;
    private final FeedbackLogRepository feedbackLogRepository;
    private final FeedbackScoreRepository feedbackScoreRepository;

    /**
     * readings가 null이면(Core API 실패) 스냅샷 없이 저장한다 — feedback_log의 snapshot/
     * outsideWeatherSnapshot은 그래서 둘 다 nullable이다. "환경 정보가 없다"와 "피드백
     * 자체가 없다"는 서로 다른 문제라 하나가 실패했다고 다른 하나까지 실패시키면 안 된다.
     *
     * @param event       원본 피드백 처리 이벤트
     * @param referenceAt 환경 스냅샷 매칭 기준 시각 (experiencedAt ?? receivedAt)
     * @param readings    Core API 조회 결과 (실내 실측값 + 외부 날씨), 실패 시 null
     */
    @Transactional
    public void persist(FeedbackProcessingEvent event, ZonedDateTime referenceAt,
                        RoomEnvironmentReadingResponse readings) {
        // 강의실 환경 조회 or 생성
        RoomEnvironmentSnapshot roomEnvironmentSnapshot = readings != null
                ? findOrCreateSnapshot(event.roomId(), referenceAt, readings)
                : null;

        // 외부 날씨 스냅샷
        OutsideWeatherSnapshot outsideWeatherSnapshot = (readings != null && readings.outsideWeather() != null)
                ? findOrCreateWeatherSnapshot(readings.outsideWeather())
                : null;

        FeedbackLog feedbackLog = feedbackLogRepository.save(FeedbackLog.builder()
                .userId(event.userId())
                .roomId(event.roomId())
                .snapshot(roomEnvironmentSnapshot)
                .outsideWeatherSnapshot(outsideWeatherSnapshot)
                .rawText(event.rawText())
                .createdAt(event.receivedAt())
                .delayed(event.isDelayed())
                .experiencedAt(event.experiencedAt())
                .build()
        );

        // sensorScores는 이미 FeedbackExtractionAgent가 "언급된 축만" 뽑아둔 결과라
        // 여기선 그대로 각각 한 row씩 저장하기만 하면 된다.
        event.sensorScores().forEach(s -> {
            FeedbackScoreId id = FeedbackScoreId.builder()
                    .sensorType(s.sensorType())
                    .build(); // feedbackLogId는 @MapsId("feedbackLogId")가 feedbackLog 연관관계에서 채워줌

            feedbackScoreRepository.save(FeedbackScore.builder()
                    .id(id)
                    .feedbackLog(feedbackLog)
                    .score(s.score())
                    .build());
        });
    }

    // Core가 넘겨주는 기상청 원본 형식(예: "33.7℃", "69%")에서 단위를 떼고 숫자만 남긴다.
    // 특정 단위 기호("℃") 하나만 잘라내는 방식은 인코딩에 따라 깨질 수 있어서,
    // 숫자/소수점/마이너스만 남기고 나머지는 전부 제거하는 쪽이 더 견고하다.
    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9.\\-]");
    private static final DateTimeFormatter BASE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId WEATHER_ZONE = ZoneId.of("Asia/Seoul"); // Core 응답에 타임존이 없어 고정 — 다지역 확장 시 가장 먼저 깨질 지점

    /**
     * 같은 시간대(Core가 알려준 실제 날씨 데이터 시각)에 여러 피드백이 몰려도
     * 외부 날씨 스냅샷은 한 번만 만든다 — room_id 없이 window_start 유니크 제약만으로
     * 재사용한다(이 배포가 서비스하는 위치는 하나뿐이라 강의실 구분이 필요 없음).
     */
    private OutsideWeatherSnapshot findOrCreateWeatherSnapshot(RoomEnvironmentReadingResponse.OutsideWeather weather) {
        ZonedDateTime windowStart = LocalDateTime.parse(weather.baseDateTime(), BASE_DATE_TIME_FORMAT).atZone(WEATHER_ZONE);
        return outsideWeatherSnapshotRepository.findByWindowStart(windowStart)
                .orElseGet(() -> outsideWeatherSnapshotRepository.save(OutsideWeatherSnapshot.builder()
                        .windowStart(windowStart)
                        .outsideTemperature(parseNumeric(weather.temperature()))
                        .outsideHumidity(parseNumeric(weather.humidity()))
                        .build()));
    }

    private BigDecimal parseNumeric(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String numericPart = NON_NUMERIC.matcher(rawValue).replaceAll("");
        return numericPart.isEmpty() ? null : new BigDecimal(numericPart);
    }


    /**
     * 15분 단위의 스냅샷이 존재하면 그 스냅샷을 가져오고 아니면 만듦
     * @param roomId 가져오고자 하는 스냅샷의 roomID
     * @param referenceAt 사용자가 피드백을 남긴 시점이거나 피드백을 남기고 싶은 시점
     * @param readings core에서 가져온 ㅡㅅ냅샷
     * @return
     */
    private RoomEnvironmentSnapshot findOrCreateSnapshot(Long roomId, ZonedDateTime referenceAt,RoomEnvironmentReadingResponse readings) {
        ZonedDateTime windowStart = floorTo15Minutes(referenceAt);
        return roomEnvironmentSnapshotRepository.findByRoomIdAndWindowStart(roomId, windowStart)
                .orElseGet(()->createSnapshot(roomId,windowStart, readings));
    }

    private RoomEnvironmentSnapshot createSnapshot(Long roomId, ZonedDateTime windowStart,RoomEnvironmentReadingResponse readings) {
        RoomEnvironmentSnapshot snapshot = roomEnvironmentSnapshotRepository.save(
                RoomEnvironmentSnapshot.builder()
                        .roomId(roomId)
                        .windowStart(windowStart)
                        .build()
        );

        readings.readings().forEach(reading -> {
            try{
                EnvironmentMetricType metricType = EnvironmentMetricType.valueOf(reading.metricType());

                // snapshotId는 @MapsId("snapshotId")가 snapshot 연관관계에서 값을 가져와 채워줌!
                RoomEnvironmentReadingId id = RoomEnvironmentReadingId.builder()
                        .metricType(metricType)
                        .build();

                roomEnvironmentReadingRepository.save(
                        RoomEnvironmentReading.builder()
                                .id(id)
                                .snapshot(snapshot)
                                .value(BigDecimal.valueOf(reading.value())) // DTO는 Double, 엔티티 컬럼은 DECIMAL
                                .build()
                );
            }catch (IllegalArgumentException e){
                log.warn("[FeedbackPersistenceService] 알 수 없는 metricType, 이 reading 건너뜀 (roomId={}, metricType={})", roomId, reading.metricType(), e);
            }
        });
        return snapshot;
    }

    /**
     * 조회할 시간을 15분 단위로 맞추기 위한 세팅임
     * 57분 -> 45분 (57 / 15 = 3, 3 * 15 = 45)
     * @param referenceAt
     * @return
     */
    private ZonedDateTime floorTo15Minutes(ZonedDateTime referenceAt) {
        int flooredMinute = (referenceAt.getMinute() / 15) * 15;
        return referenceAt.withMinute(flooredMinute).withSecond(0).withNano(0);
    }
}
