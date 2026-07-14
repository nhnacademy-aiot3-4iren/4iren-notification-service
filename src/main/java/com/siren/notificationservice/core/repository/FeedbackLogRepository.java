package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.FeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {
}
