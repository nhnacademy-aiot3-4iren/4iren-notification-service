package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.repository.AlertHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertHistoryRetentionServiceTest {

    private static final int RETENTION_DAYS = 365;
    private static final int BATCH_SIZE = 2000;

    private final AlertHistoryRepository alertHistoryRepository = mock(AlertHistoryRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final AlertHistoryRetentionService retentionService =
            new AlertHistoryRetentionService(alertHistoryRepository, transactionManager, RETENTION_DAYS, BATCH_SIZE);

    @Test
    void purgeDeletesInChunksUntilPartialPage() {
        // TransactionTemplate이 콜백을 실행하도록 트랜잭션 시작만 스텁 (실제 DB 불필요)
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        // 1차: batchSize 만큼 삭제(계속) -> 2차: batchSize 미만(마지막 페이지 -> 종료)
        when(alertHistoryRepository.deleteBatch(any(LocalDateTime.class), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE, 500);

        retentionService.purge();

        verify(alertHistoryRepository, times(2)).deleteBatch(any(LocalDateTime.class), eq(BATCH_SIZE));
    }

    @Test
    void purgeStopsAfterOneCallWhenNothingToDelete() {
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        // 지울 게 없으면 첫 배치가 0건 -> 한 번만 호출하고 종료
        when(alertHistoryRepository.deleteBatch(any(LocalDateTime.class), eq(BATCH_SIZE)))
                .thenReturn(0);

        retentionService.purge();

        verify(alertHistoryRepository, times(1)).deleteBatch(any(LocalDateTime.class), eq(BATCH_SIZE));
    }
}
