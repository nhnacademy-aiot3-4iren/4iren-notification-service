package com.siren.notificationservice.core.scheduler;

import com.siren.notificationservice.core.service.FeedbackLogRetentionService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackLogRetentionScheduler {
    private final FeedbackLogRetentionService feedbackLogRetentionService;

    @Scheduled(cron = "${feedback-log.retention-cron}")
    @SchedulerLock(name = "feedbackLogRetention", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M") // lockAtMostFor: 이 시간이 지나면 강제 해제 (인스턴스 죽어도 RedisTTL로 자동만료) , lockAtLeatFor: 빨리끝나도 최소 유지
    public void purgeOldFeedbackLogs() {
        feedbackLogRetentionService.purgeFeedbackLogs();
    }
}
