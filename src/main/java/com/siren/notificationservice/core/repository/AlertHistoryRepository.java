package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long>, AlertHistoryRepositoryCustom {

    List<AlertHistory> findByEventId(String eventId);

    /**
     * 알림 이력 단건을 요청자 소유인지까지 확인해 조회한다. 남의 이력 조회(IDOR) 방지용으로 id 단독이 아닌 userId까지 스코프한다.
     */
    Optional<AlertHistory> findByAlertHistoryIdAndUserId(Long alertHistoryId, Long userId);

    /**
     * AlertHistory 삭제 주기 배치
     */
    @Modifying
    @Query(value = "DELETE FROM alert_history WHERE send_at < :cutoff LIMIT :batchSize", nativeQuery = true)
    int deleteBatch(@Param("cutoff") ZonedDateTime cutoff, @Param("batchSize") int batchSize);

    /**
     * filter option 보여주기
     */
    @Query("SELECT DISTINCT a.alertType FROM AlertHistory a WHERE a.userId = :userId AND a.alertType IS NOT NULL")
    List<AlertType> findAlertTypesByUserId(@Param("userId") Long userId);
}
