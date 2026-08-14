package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.FeedbackLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {
    List<FeedbackLog> findByFeedbackLogIdGreaterThanOrderByFeedbackLogIdAsc(Long sinceId, Pageable pageable);

    @Query(value = "SELECT feedback_log_id FROM feedback_log WHERE created_at < :cutoff LIMIT :batchSize", nativeQuery = true)
    List<Long> findOldLogIds(@Param("cutoff")LocalDateTime cutoff, @Param("batchSize")int batchSize);

    @Modifying
    @Query(value = "DELETE FROM feedback_log WHERE feedback_log_id IN (:ids)",nativeQuery = true)
    void deleteLogsByIds(@Param("ids") List<Long> ids);
}
