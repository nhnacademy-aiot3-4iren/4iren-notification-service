package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.EnrichedFeedbackLog;
import com.siren.notificationservice.core.dto.response.FeedbackExportResult;
import com.siren.notificationservice.core.entity.table.FeedbackLog;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackExportServiceTest {

    private final FeedbackExportQueryService feedbackExportQueryService = mock(FeedbackExportQueryService.class);
    private final FeedbackCsvConverter feedbackCsvConverter = mock(FeedbackCsvConverter.class);
    private final FeedbackExportService feedbackExportService =
            new FeedbackExportService(feedbackExportQueryService, feedbackCsvConverter);

    @Test
    void exportAsCsvReturnsCsvAndLastFetchedId() {
        FeedbackLog log = FeedbackLog.builder()
                .feedbackLogId(5L).roomId(7L).rawText("더워요").createdAt(ZonedDateTime.now()).userId(1L).build();
        EnrichedFeedbackLog enriched = new EnrichedFeedbackLog(log, List.of(), List.of(), null);
        when(feedbackExportQueryService.fetch(0L, 10)).thenReturn(List.of(enriched));
        when(feedbackCsvConverter.toCsv(List.of(enriched))).thenReturn("csv-data");

        FeedbackExportResult result = feedbackExportService.exportAsCsv(0L, 10);

        assertThat(result.csv()).isEqualTo("csv-data");
        assertThat(result.lastFeedbackLogId()).isEqualTo(5L);
    }

    @Test
    void exportAsCsvKeepsSinceIdAsCursorWhenNothingFetched() {
        when(feedbackExportQueryService.fetch(100L, 10)).thenReturn(List.of());
        when(feedbackCsvConverter.toCsv(List.of())).thenReturn("");

        FeedbackExportResult result = feedbackExportService.exportAsCsv(100L, 10);

        assertThat(result.csv()).isEmpty();
        assertThat(result.lastFeedbackLogId()).isEqualTo(100L);
    }
}
