package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackScoreRepository extends JpaRepository<FeedbackScore, FeedbackScoreId> {

    /**
     * 특정 피드백 로그(EmbeddedId의 feedbackLogId)에 딸린 점수 전체를 조회한다.
     */
    List<FeedbackScore> findById_FeedbackLogId(Long feedbackLogId);

    /**
     * feedbackLogId 목록에 딸린 점수 전체를 한 번에 조회한다.
     */
    List<FeedbackScore> findById_FeedbackLogIdIn(List<Long> feedbackLogIds);
}
