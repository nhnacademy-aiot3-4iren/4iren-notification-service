package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.dto.request.AlertHistorySearchCondition;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlertHistoryRepositoryCustom {
    Page<AlertHistory> search(Long userId, AlertHistorySearchCondition filter, Pageable pageable);
}
