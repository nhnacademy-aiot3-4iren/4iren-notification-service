package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import com.siren.notificationservice.core.repository.FeedbackLogRepository;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * FeedbackLog에 대한 순수 DB 접근 계층. 오케스트레이션 로직은 호출하는 쪽의 책임이다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackLogService {
    private final FeedbackLogRepository feedbackLogRepository;
    private final FeedbackScoreService feedbackScoreService;

    /**
     * feedbackLogId로 피드백 로그 단건을 조회한다.
     */
    @Transactional(readOnly = true)
    public FeedbackLog getFeedbackLog(Long feedbackLogId) {
        Objects.requireNonNull(feedbackLogId, "feedbackLogId는 null일 수 없습니다.");
        return feedbackLogRepository.findById(feedbackLogId).orElse(null);
    }

    private FeedbackLog createFeedbackLog(Long userId, Long roomId, RoomEnvironmentSnapshot snapshot,
                                           OutsideWeatherSnapshot outsideWeatherSnapshot, String rawText,
                                           ZonedDateTime createdAt, boolean delayed, ZonedDateTime experiencedAt) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        Objects.requireNonNull(rawText, "rawText는 null일 수 없습니다.");
        Objects.requireNonNull(createdAt, "createdAt은 null일 수 없습니다.");
        return feedbackLogRepository.save(FeedbackLog.builder()
                .userId(userId)
                .roomId(roomId)
                .snapshot(snapshot)
                .outsideWeatherSnapshot(outsideWeatherSnapshot)
                .rawText(rawText)
                .createdAt(createdAt)
                .delayed(delayed)
                .experiencedAt(experiencedAt)
                .build());
    }

    /**
     * FeedbackLog와 FeedbackScore들을 하나의 트랜잭션으로 저장한다(둘이 원자적으로 묶여야 함).
     */
    @Transactional
    public void createFeedbackLogWithScores(Long userId, Long roomId, RoomEnvironmentSnapshot snapshot,
                                                    OutsideWeatherSnapshot outsideWeatherSnapshot, String rawText,
                                                    ZonedDateTime createdAt, boolean delayed, ZonedDateTime experiencedAt,
                                                    List<FeedbackExtractionResult.SensorScore> sensorScores) {
        Objects.requireNonNull(sensorScores, "sensorScores는 null일 수 없습니다.");
        FeedbackLog feedbackLog = createFeedbackLog(userId, roomId, snapshot, outsideWeatherSnapshot, rawText, createdAt, delayed, experiencedAt);

        // sensorScores는 이미 FeedbackExtractionAgent가 "언급된 축만" 뽑아둔 결과라
        // 여기선 그대로 각각 한 row로 매핑해서 한 번에 저장하기만 하면 된다.
        List<FeedbackScore> feedbackScores = sensorScores.stream()
                .map(s -> FeedbackScore.builder()
                        .id(FeedbackScoreId.builder().sensorType(s.sensorType()).build()) // feedbackLogId는 @MapsId("feedbackLogId")가 feedbackLog 연관관계에서 채워줌
                        .feedbackLog(feedbackLog)
                        .score(s.score())
                        .build())
                .toList();
        feedbackScoreService.saveAll(feedbackScores);
    }

    /**
     * sinceId 이후의 피드백 로그를 id 오름차순으로 최대 limit개 조회한다.
     */
    @Transactional(readOnly = true)
    public List<FeedbackLog> getFeedbackLogs(Long sinceId, int limit) {
        Objects.requireNonNull(sinceId, "sinceId는 null일 수 없습니다.");
        if (limit <= 0) {
            log.warn("[FeedbackLogService] limit이 음수값으로 들어와 1로 보정합니다 (limit={})", limit);
            limit = 1;
        }
        return feedbackLogRepository.findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(sinceId, PageRequest.of(0, limit));
    }
}
