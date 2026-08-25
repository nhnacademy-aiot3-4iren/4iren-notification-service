package com.siren.notificationservice.core.service.basic_service;

import com.siren.notificationservice.core.dto.FeedbackLogCreateRequest;
import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.repository.FeedbackLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * FeedbackLog에 대한 순수 DB 접근 계층. 오케스트레이션 로직은 호출하는 쪽의 책임이다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackLogService {
    private static final String FEEDBACK_LOG_ID_NULL_MESSAGE = "feedbackLogId는 null일 수 없습니다.";
    private static final String SINCE_ID_NULL_MESSAGE = "sinceId는 null일 수 없습니다.";

    private final FeedbackLogRepository feedbackLogRepository;
    private final FeedbackScoreService feedbackScoreService;

    /**
     * feedbackLogId로 피드백 로그 단건을 조회한다.
     */
    @Transactional(readOnly = true)
    public FeedbackLog getFeedbackLog(Long feedbackLogId) {
        Objects.requireNonNull(feedbackLogId, FEEDBACK_LOG_ID_NULL_MESSAGE);
        return feedbackLogRepository.findById(feedbackLogId).orElse(null);
    }

    private FeedbackLog createFeedbackLog(FeedbackLogCreateRequest request) {
        return feedbackLogRepository.save(FeedbackLog.builder()
                .userId(request.userId())
                .roomId(request.roomId())
                .snapshot(request.snapshot())
                .outsideWeatherSnapshot(request.outsideWeatherSnapshot())
                .rawText(request.rawText())
                .createdAt(request.createdAt())
                .delayed(request.delayed())
                .experiencedAt(request.experiencedAt())
                .build());
    }

    /**
     * FeedbackLog와 FeedbackScore들을 하나의 트랜잭션으로 저장한다(둘이 원자적으로 묶여야 함).
     * 필수값 검증은 FeedbackLogCreateRequest 생성 시점에 이미 끝나 있다.
     */
    @Transactional
    public void createFeedbackLogWithScores(FeedbackLogCreateRequest request) {
        FeedbackLog feedbackLog = createFeedbackLog(request);

        // sensorScores는 이미 FeedbackExtractionAgent가 "언급된 축만" 뽑아둔 결과라
        // 여기선 그대로 각각 한 row로 매핑해서 한 번에 저장하기만 하면 된다.
        List<FeedbackScore> feedbackScores = request.sensorScores().stream()
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
        Objects.requireNonNull(sinceId, SINCE_ID_NULL_MESSAGE);
        if (limit <= 0) {
            log.warn("[FeedbackLogService] limit이 음수값으로 들어와 1로 보정합니다 (limit={})", limit);
            limit = 1;
        }
        return feedbackLogRepository.findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(sinceId, PageRequest.of(0, limit));
    }
}
