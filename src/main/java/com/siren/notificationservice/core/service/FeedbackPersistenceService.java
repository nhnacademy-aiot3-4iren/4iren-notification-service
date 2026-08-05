package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.client.CoreApiClient;
import com.siren.notificationservice.core.dto.RoomWeatherRegion;
import com.siren.notificationservice.core.dto.response.OutsideWeather;
import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import com.siren.notificationservice.core.service.cache.RoomWeatherRegionCacheService;
import com.siren.notificationservice.telegram.dto.event.FeedbackProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * FeedbackProcessingEvent를 DB 레코드로 저장한다. Core API를 직접 호출하기 때문에 persist()엔
 * 의도적으로 @Transactional이 없다
 * 원자성이 필요한 부분은 각 서비스로직에서 트랜잭션을 관리하도록 한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackPersistenceService {
    private final RoomEnvironmentSnapshotService roomEnvironmentSnapshotService;
    private final OutsideWeatherSnapshotService outsideWeatherSnapshotService;
    private final RoomWeatherRegionCacheService regionCacheService;
    private final FeedbackLogService feedbackLogService;
    private final CoreApiClient coreApiClient;

    /**
     * 피드백을 DB에 저장한다. 강의실 실측값/외부 날씨 조회는 각각 독립적으로 실패할 수 있고,
     * 실패해도 스냅샷 없이 피드백 저장은 계속 진행한다.
     */
    public void persist(FeedbackProcessingEvent event, ZonedDateTime referenceAt) {
        // 강의실 환경 조회 or 생성
        RoomEnvironmentSnapshot sensorSnapshot = findRoomEnvironmentSnapshot(event.roomId(), referenceAt);
        // 외부 날씨 조회
        OutsideWeatherSnapshot outsideSnapshot = findOutsideWeatherSnapshot(event.roomId(), referenceAt);

        // event의 시각들은 큐 안에서만 도는 LocalDateTime이지만, FeedbackLog 엔티티 컬럼은
        // ZonedDateTime이라 여기(DB로 넘어가는 경계)서 변환한다.
        ZonedDateTime createdAt = event.receivedAt().atZone(SERVICE_ZONE);
        ZonedDateTime experiencedAt = event.experiencedAt() != null ? event.experiencedAt().atZone(SERVICE_ZONE) : null;

        feedbackLogService.createFeedbackLogWithScores(
                event.userId(),
                event.roomId(),
                sensorSnapshot,
                outsideSnapshot,
                event.rawText(),
                createdAt,
                event.isDelayed(),
                experiencedAt,
                event.sensorScores()
        );
    }

    private RoomEnvironmentSnapshot findRoomEnvironmentSnapshot(Long roomId, ZonedDateTime referenceAt) {
        RoomEnvironmentSnapshot sensorSnapshot = roomEnvironmentSnapshotService.findSnapshotId(roomId, referenceAt);

        if(sensorSnapshot == null) {
            RoomEnvironmentReadingResponse readings = fetchOrNull("강의실 실측값", roomId, referenceAt,
                    ()-> coreApiClient.getRoomSensorsReadings(roomId, referenceAt.toLocalDateTime()));
            sensorSnapshot = readings != null
                    ? roomEnvironmentSnapshotService.createWithReadings(roomId, referenceAt, readings)
                    : null;
        }
        return sensorSnapshot;
    }

    /**
     * 캐시에서 지역의 nx,ny를 찾고
     * 있으면 -> 스냅샷 조회 -> 리턴
     * 없으면 -> Core 호출
     *
     * 스냅샷 조회시
     * 있으면 -> 리턴
     * 없으면 -> Core 호출
     */
    private OutsideWeatherSnapshot findOutsideWeatherSnapshot(Long roomId, ZonedDateTime referenceAt) {
        RoomWeatherRegion region = regionCacheService.find(roomId).orElse(null);
        OutsideWeatherSnapshot outsideSnapshot = null;

        if(region != null) {
            outsideSnapshot = outsideWeatherSnapshotService.findSnapshotId(region.nx(), region.ny(), referenceAt);
        }

        if(outsideSnapshot == null) {
            OutsideWeather weather = fetchOrNull("외부 날씨", roomId, referenceAt,
                    ()-> coreApiClient.getOutsideWeather(roomId, referenceAt.toLocalDateTime()));
            if(weather != null) {
                outsideSnapshot = createWeatherSnapshot(weather);
            }

            if(region==null && weather!=null) {
                regionCacheService.save(roomId, new RoomWeatherRegion(weather.nx(),weather.ny()));
            }
        }
        return outsideSnapshot;
    }

    // Core가 넘겨주는 기상청 원본 형식(예: "33.7℃", "69%")에서 단위를 떼고 숫자만 남긴다.
    // 특정 단위 기호("℃") 하나만 잘라내는 방식은 인코딩에 따라 깨질 수 있어서,
    // 숫자/소수점/마이너스만 남기고 나머지는 전부 제거하는 쪽으로
    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9.\\-]");
    private static final DateTimeFormatter BASE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul"); // Core 응답/이벤트 시각에 타임존이 없어 고정 — 다지역 확장 시 가장 먼저 깨질 지점

    /**
     * 같은 지역(nx, ny)·같은 시간대 스냅샷은 강의실이 달라도 공유 재사용한다. 생성 전에 한 번
     * 더 확인하는 이유: 다른 강의실이 그 사이 먼저 만들었을 수 있어서(중복 생성 예외 방지).
     */
    private OutsideWeatherSnapshot createWeatherSnapshot(OutsideWeather weather) {
        ZonedDateTime windowStart;
        try {
            windowStart = LocalDateTime.parse(weather.baseDateTime(), BASE_DATE_TIME_FORMAT).atZone(SERVICE_ZONE);
        } catch (DateTimeParseException e) {
            // baseDateTime 형식이 예상과 다르면 이 피드백의 날씨 스냅샷만 포기한다.
            log.warn("[FeedbackPersistenceService] baseDateTime 파싱 실패, 외부 날씨 스냅샷 생략 (baseDateTime={})", weather.baseDateTime(), e);
            return null;
        }

        ZonedDateTime finalWindowStart = windowStart;
        OutsideWeatherSnapshot snapshot = outsideWeatherSnapshotService.findSnapshotId(weather.nx(), weather.ny(), finalWindowStart);

        return snapshot != null ? snapshot : outsideWeatherSnapshotService.createOutsideWeatherSnapshot(weather.nx(), weather.ny(), finalWindowStart, parseNumeric(weather.temperature()), parseNumeric(weather.humidity()));
    }

    private BigDecimal parseNumeric(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String numericPart = NON_NUMERIC.matcher(rawValue).replaceAll("");
        if (numericPart.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(numericPart);
        } catch (NumberFormatException e) {
            // Core가 "-"처럼 단위만 떼면 숫자가 안 되는 결측치 표기를 보낼 수 있음 -> 값 하나 생략,
            // 피드백 전체를 실패시키지 않음.
            log.warn("[FeedbackPersistenceService] 숫자 파싱 실패, 값 생략 (rawValue={})", rawValue, e);
            return null;
        }
    }

    private <T> T fetchOrNull(String label, Long roomId, ZonedDateTime referenceAt, Supplier<T> call) {
        try{
            return call.get();
        }catch (CoreApiUnavailableException e){
            /// core에서 환경 스냅샷을 못 받아와도 피드백 자체를 버리면 안됨 따라서 로그만 찍음
            log.warn("[FeedbackPersistenceService] {} 조회 실패, 스냅샷 없이 저장 (roomId={}, referenceAt={})", label ,roomId, referenceAt, e);
            return null;
        }
    }
}
