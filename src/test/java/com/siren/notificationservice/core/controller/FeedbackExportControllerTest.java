package com.siren.notificationservice.core.controller;

import com.siren.notificationservice.core.dto.response.FeedbackExportResult;
import com.siren.notificationservice.core.service.FeedbackExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackExportController.class)
class FeedbackExportControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FeedbackExportService feedbackExportService;

    @Test
    void getFeedbackExports() throws Exception {
        Long expectedLastId = 150L;
        String mockCsvData = "feedback_log_id,room_id,reference_at,metric_type,value\n1,101,5,2026-08-04\n";

        FeedbackExportResult mockResult = new FeedbackExportResult(mockCsvData, expectedLastId);

        when(feedbackExportService.exportAsCsv(anyLong(), anyInt())).thenReturn(mockResult);

        mockMvc.perform(get("/api/notification/internal/feedback-exports")
                        .param("sinceId", "0")
                        .param("limit", "5000"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().string("X-Last-Feedback-Log-Id", String.valueOf(expectedLastId)))
                .andExpect(content().string(mockCsvData));
    }
}