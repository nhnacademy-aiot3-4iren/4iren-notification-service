package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.repository.FeedbackLogRepository;
import com.siren.notificationservice.core.repository.OutsideWeatherSnapshotRepository;
import com.siren.notificationservice.core.repository.RoomEnvironmentSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@Slf4j
public class FeedbackLogRetentionService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final FeedbackLogRepository feedbackLogRepository;
    private final RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository;
    private final OutsideWeatherSnapshotRepository outsideWeatherSnapshotRepository;
    private final TransactionTemplate transactionTemplate;
    private final int retentionDays;
    private final int batchSize;

    public FeedbackLogRetentionService(FeedbackLogRepository feedbackLogRepository,
                                       RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository,
                                       OutsideWeatherSnapshotRepository outsideWeatherSnapshotRepository,
                                       PlatformTransactionManager transactionManager,
                                       @Value("${feedback-log.retention-days}") int retentionDays,
                                       @Value("${feedback-log.retention-batch-size}") int batchSize) {
        this.feedbackLogRepository = feedbackLogRepository;
        this.roomEnvironmentSnapshotRepository = roomEnvironmentSnapshotRepository;
        this.outsideWeatherSnapshotRepository = outsideWeatherSnapshotRepository;
        // 배치마다 독립 트랜잭션을 열려고 TransactionTemplate을 직접 구성한다(서비스에 @Transactional을 걸면
        // 루프 전체가 한 트랜잭션이 돼서 청크 삭제의 의미가 사라짐).
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
    }

    /**
     * 보관 기간이 지난 feedback_log를 지우고, 그로 인해 참조가 끊긴 고아 스냅샷(실내 환경/외부 날씨)까지 정리한다.
     */
    public void purgeFeedbackLogs() {
        ZonedDateTime cutoff = ZonedDateTime.now(ZONE).minusDays(retentionDays);
        purgeOldLogs(cutoff);                    // 1) 오래된 feedback_log (feedback_score는 V16 cascade로 함께 삭제)
        purgeOrphanRoomEnvironmentSnapshots();   // 2) 참조 끊긴 실내 환경 스냅샷 (reading은 V16 cascade로 함께 삭제)
        purgeOrphanOutsideWeatherSnapshots();    // 3) 참조 끊긴 외부 날씨 스냅샷
    }

    /**
     * 보관 기간이 지난 feedback_log를 청크 단위로 삭제한다. 각 청크는 독립 트랜잭션이라 대량 삭제여도 롱 트랜잭션, 락 홀드, 복제 지연을 피한다.
     */
    private void purgeOldLogs(ZonedDateTime cutoff) {
        int total = 0;
        while (true) {
            List<Long> ids = feedbackLogRepository.findOldLogIds(cutoff, batchSize);
            if (ids.isEmpty()) {
                break;
            }
            transactionTemplate.executeWithoutResult(status -> feedbackLogRepository.deleteLogsByIds(ids));
            total += ids.size();
        }
        log.info("[FeedbackLogRetentionService] feedback_log {}건 삭제 (cutoff={})", total, cutoff);
    }

    /**
     * 아무 feedback_log도 참조하지 않는 고아 room_environment_snapshot을 청크 단위로 삭제한다(reading은 FK ON DELETE CASCADE로 함께 삭제).
     */
    private void purgeOrphanRoomEnvironmentSnapshots() {
        int total = 0;
        while (true) {
            List<Long> ids = roomEnvironmentSnapshotRepository.findOrphanSnapshotIds(batchSize);
            if (ids.isEmpty()) {
                break;
            }
            transactionTemplate.executeWithoutResult(status -> roomEnvironmentSnapshotRepository.deleteSnapshotsByIds(ids));
            total += ids.size();
        }
        log.info("[FeedbackLogRetentionService] 고아 room_environment_snapshot {}건 삭제", total);
    }

    /**
     * 아무 feedback_log도 참조하지 않는 고아 outside_weather_snapshot을 청크 단위로 삭제한다.
     */
    private void purgeOrphanOutsideWeatherSnapshots() {
        int total = 0;
        while (true) {
            List<Long> ids = outsideWeatherSnapshotRepository.findOrphanWeatherSnapshotIds(batchSize);
            if (ids.isEmpty()) {
                break;
            }
            transactionTemplate.executeWithoutResult(status -> outsideWeatherSnapshotRepository.deleteWeatherSnapshotsByIds(ids));
            total += ids.size();
        }
        log.info("[FeedbackLogRetentionService] 고아 outside_weather_snapshot {}건 삭제", total);
    }
}
