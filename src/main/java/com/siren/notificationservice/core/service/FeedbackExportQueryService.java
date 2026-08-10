package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.EnrichedFeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.service.basic_service.FeedbackLogService;
import com.siren.notificationservice.core.service.basic_service.FeedbackScoreService;
import com.siren.notificationservice.core.service.basic_service.OutsideWeatherSnapshotService;
import com.siren.notificationservice.core.service.basic_service.RoomEnvironmentReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * feedback-exports에 필요한 데이터를 조회만 담당한다. feedback_log를 조회한 뒤 필요한
 * id(feedbackLogId/snapshotId/weatherSnapshotId)를 모아 배치로 한 번씩만 조회해서
 * (N+1 방지) EnrichedFeedbackLog로 묶어 반환한다. CSV 변환 책임은 FeedbackCsvConverter가 갖는다.
 */
@Service
@RequiredArgsConstructor
public class FeedbackExportQueryService {

    private final FeedbackLogService feedbackLogService;
    private final FeedbackScoreService feedbackScoreService;
    private final RoomEnvironmentReadingService roomEnvironmentReadingService;
    private final OutsideWeatherSnapshotService outsideWeatherSnapshotService;

    /**
     * sinceId 이후의 피드백을 최대 limit건 조회해서 각 피드백에 딸린 점수/실내값/외부날씨를
     * 배치 조회로 묶어 EnrichedFeedbackLog 목록으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<EnrichedFeedbackLog> fetch(Long sinceId, int limit) {
        Objects.requireNonNull(sinceId, "sinceId는 null일 수 없습니다.");
        List<FeedbackLog> logs = feedbackLogService.getFeedbackLogs(sinceId, limit);
        if (logs.isEmpty()) {
            return List.of();
        }

        List<Long> feedbackLogIds = logs.stream()
                .map(FeedbackLog::getFeedbackLogId)
                .toList();
        List<Long> snapshotIds = logs.stream()
                .map(FeedbackLog::getSnapshot)
                .filter(Objects::nonNull)
                .map(RoomEnvironmentSnapshot::getSnapshotId)
                .distinct()
                .toList();
        List<Long> weatherIds = logs.stream()
                .map(FeedbackLog::getOutsideWeatherSnapshot)
                .filter(Objects::nonNull)
                .map(OutsideWeatherSnapshot::getWeatherSnapshotId)
                .distinct()
                .toList();

        Map<Long, List<FeedbackScore>> scoresByLogId = feedbackScoreService.getScoresByFeedbackLogIdIn(feedbackLogIds).stream()
                .collect(Collectors.groupingBy(score -> score.getId().getFeedbackLogId()));
        Map<Long, List<RoomEnvironmentReading>> readingsBySnapshotId = roomEnvironmentReadingService.getReadingsBySnapshotIds(snapshotIds).stream()
                .collect(Collectors.groupingBy(reading -> reading.getId().getSnapshotId()));
        Map<Long, OutsideWeatherSnapshot> weatherById = outsideWeatherSnapshotService.getByIds(weatherIds).stream()
                .collect(Collectors.toMap(OutsideWeatherSnapshot::getWeatherSnapshotId, Function.identity()));

        return logs.stream()
                .map(log -> new EnrichedFeedbackLog(
                        log,
                        scoresByLogId.getOrDefault(log.getFeedbackLogId(), List.of()),
                        log.getSnapshot() != null
                                ? readingsBySnapshotId.getOrDefault(log.getSnapshot().getSnapshotId(), List.of())
                                : List.of(),
                        log.getOutsideWeatherSnapshot() != null
                                ? weatherById.get(log.getOutsideWeatherSnapshot().getWeatherSnapshotId())
                                : null
                ))
                .toList();
    }
}
