package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.repository.FeedbackLogRepository;
import com.siren.notificationservice.core.repository.OutsideWeatherSnapshotRepository;
import com.siren.notificationservice.core.repository.RoomEnvironmentSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeedbackLogRetentionServiceTest {

    private static final int RETENTION_DAYS = 90;
    private static final int BATCH_SIZE = 2000;

    private final FeedbackLogRepository feedbackLogRepository = mock(FeedbackLogRepository.class);
    private final RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository = mock(RoomEnvironmentSnapshotRepository.class);
    private final OutsideWeatherSnapshotRepository outsideWeatherSnapshotRepository = mock(OutsideWeatherSnapshotRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final FeedbackLogRetentionService retentionService = new FeedbackLogRetentionService(
            feedbackLogRepository, roomEnvironmentSnapshotRepository, outsideWeatherSnapshotRepository,
            transactionManager, RETENTION_DAYS, BATCH_SIZE);

    @Test
    void purgeDeletesOldLogsThenOrphanSnapshotsInChunks() {
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        List<Long> logBatch = List.of(1L, 2L, 3L);
        List<Long> roomBatch = List.of(10L, 11L);
        List<Long> weatherBatch = List.of(20L);
        // 각 조회: 1차 배치 반환(계속) -> 2차 빈 리스트(종료)
        when(feedbackLogRepository.findOldLogIds(any(LocalDateTime.class), eq(BATCH_SIZE)))
                .thenReturn(logBatch, List.of());
        when(roomEnvironmentSnapshotRepository.findOrphanSnapshotIds(BATCH_SIZE))
                .thenReturn(roomBatch, List.of());
        when(outsideWeatherSnapshotRepository.findOrphanWeatherSnapshotIds(BATCH_SIZE))
                .thenReturn(weatherBatch, List.of());

        retentionService.purgeFeedbackLogs();

        // feedback_log 삭제 -> 고아 room 스냅샷 -> 고아 weather 스냅샷 (자식은 전부 DB cascade로 자동)
        verify(feedbackLogRepository).deleteLogsByIds(logBatch);
        verify(roomEnvironmentSnapshotRepository).deleteSnapshotsByIds(roomBatch);
        verify(outsideWeatherSnapshotRepository).deleteWeatherSnapshotsByIds(weatherBatch);
    }

    @Test
    void purgeDoesNothingWhenNothingToDelete() {
        // 지울 게 하나도 없으면 각 조회가 빈 리스트 -> 삭제/트랜잭션 없이 종료
        when(feedbackLogRepository.findOldLogIds(any(LocalDateTime.class), eq(BATCH_SIZE))).thenReturn(List.of());
        when(roomEnvironmentSnapshotRepository.findOrphanSnapshotIds(BATCH_SIZE)).thenReturn(List.of());
        when(outsideWeatherSnapshotRepository.findOrphanWeatherSnapshotIds(BATCH_SIZE)).thenReturn(List.of());

        retentionService.purgeFeedbackLogs();

        verify(feedbackLogRepository, never()).deleteLogsByIds(any());
        verify(roomEnvironmentSnapshotRepository, never()).deleteSnapshotsByIds(any());
        verify(outsideWeatherSnapshotRepository, never()).deleteWeatherSnapshotsByIds(any());
        verifyNoInteractions(transactionManager);
    }
}
