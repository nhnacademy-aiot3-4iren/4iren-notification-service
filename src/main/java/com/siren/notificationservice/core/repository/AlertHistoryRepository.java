package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    /**
     * 이 유저에게 발송된 알림 이력을 페이지 단위로 조회한다. 공동 관리자여도 서로의 수신 이력은 안 보인다.
     */
    Page<AlertHistory> findByUserId(Long userId, Pageable pageable);

    List<AlertHistory> findByEventId(String eventId);

    /**
     * 알림 이력 단건을 요청자 소유인지까지 확인해 조회한다. 남의 이력 조회(IDOR) 방지용으로 id 단독이 아닌 userId까지 스코프한다.
     */
    Optional<AlertHistory> findByAlertHistoryIdAndUserId(Long alertHistoryId, Long userId);
}
