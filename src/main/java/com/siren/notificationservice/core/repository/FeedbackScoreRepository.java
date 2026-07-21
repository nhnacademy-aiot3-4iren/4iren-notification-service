package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.domain.FeedbackScoreId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackScoreRepository extends JpaRepository<FeedbackScore, FeedbackScoreId> {
}
