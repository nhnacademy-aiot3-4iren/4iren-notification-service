package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.repository.AlertHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@Slf4j
public class AlertHistoryRetentionService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final AlertHistoryRepository alertHistoryRepository;
    private final TransactionTemplate transactionTemplate;
    private final int retentionDays;
    private final int batchSize;

    public AlertHistoryRetentionService(AlertHistoryRepository alertHistoryRepository,
                                        PlatformTransactionManager transactionManager,
                                        @Value("${alert-history.retention-days}") int retentionDays,
                                        @Value("${alert-history.retention-batch-size}") int batchSize) {
        this.alertHistoryRepository = alertHistoryRepository;
        // 배치마다 독립 트랜잭션을 열려고 TransactionTemplate을 직접 구성한다(서비스에 @Transactional을 걸면
        // 루프 전체가 한 트랜잭션이 돼서 청크 삭제의 의미가 사라짐).
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
    }

    /**
     * 보관 기간이 지난 alert_history를 청크 단위로 삭제한다. 각 청크는 독립 트랜잭션이라 대량 삭제여도 롱 트랜잭션, 락 홀드, 복제 지연을 피한다.
     */
    public void purge() {
        ZonedDateTime cutoff = ZonedDateTime.now(ZONE).minusDays(retentionDays);
        int total = 0;
        int deleted;
        do {
            // execute() 한 번이 곧 트랜잭션 하나. 배치 삭제 후 즉시 커밋되고 다음 배치로 넘어간다.
            deleted = transactionTemplate.execute(status -> alertHistoryRepository.deleteBatch(cutoff, batchSize));
            total += deleted;
        } while (deleted == batchSize); // batchSize보다 적게 지워졌으면 마지막 페이지 -> 종료
        log.info("[AlertHistoryRetention] alert_history {}건 삭제 (cutoff={}, batchSize={})", total, cutoff, batchSize);
    }
}
