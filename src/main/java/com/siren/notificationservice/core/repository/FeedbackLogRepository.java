package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.FeedbackLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {
    List<FeedbackLog> findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(Long sinceId, Pageable pageable);
}
