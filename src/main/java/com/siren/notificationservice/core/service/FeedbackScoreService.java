package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.repository.FeedbackScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * FeedbackScore에 대한 순수 DB 접근 계층. sensorScores를 엔티티로 매핑하는 로직은
 * 이 클래스가 아니라 호출하는 쪽(FeedbackPersistenceService 등)의 책임이다.
 */
@Service
@RequiredArgsConstructor
public class FeedbackScoreService {
    private final FeedbackScoreRepository feedbackScoreRepository;

    /**
     * 특정 피드백 로그에 딸린 축별 점수 전체를 조회한다.
     */
    @Transactional(readOnly = true)
    public List<FeedbackScore> getScoresByFeedbackLogId(Long feedbackLogId) {
        Objects.requireNonNull(feedbackLogId, "feedbackLogId는 null일 수 없습니다.");
        return feedbackScoreRepository.findById_FeedbackLogId(feedbackLogId);
    }

    /**
     * 점수들을 한 번에 저장한다. REQUIRED라 호출부(createFeedbackLogWithScores)의
     * REQUIRES_NEW 트랜잭션에 합류한다.
     */
    @Transactional
    public List<FeedbackScore> saveAll(List<FeedbackScore> scores) {
        Objects.requireNonNull(scores, "scores는 null일 수 없습니다.");
        return feedbackScoreRepository.saveAll(scores);
    }
}
