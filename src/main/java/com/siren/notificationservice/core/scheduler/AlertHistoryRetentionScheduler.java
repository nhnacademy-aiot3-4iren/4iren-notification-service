package com.siren.notificationservice.core.scheduler;

import com.siren.notificationservice.core.service.AlertHistoryRetentionService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertHistoryRetentionScheduler {
    private final AlertHistoryRetentionService alertHistoryRetentionService;

    @Scheduled(cron = "${alert-history.retention-cron}")
    @SchedulerLock(name = "alertHistoryRetention", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M") // lockAtMostFor: 이 시간이 지나면 강제 해제 (인스턴스 죽어도 RedisTTL로 자동만료) , lockAtLeatFor: 빨리끝나도 최소 유지
    public void purgeOldAlertHistory() {
        alertHistoryRetentionService.purge();
    }
}
