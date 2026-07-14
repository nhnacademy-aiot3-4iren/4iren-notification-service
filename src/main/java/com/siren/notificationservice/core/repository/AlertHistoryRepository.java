package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
}
