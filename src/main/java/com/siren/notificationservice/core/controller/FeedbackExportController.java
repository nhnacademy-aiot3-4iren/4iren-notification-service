package com.siren.notificationservice.core.controller;

import com.siren.notificationservice.core.controller.doc.FeedbackExportControllerDoc;
import com.siren.notificationservice.core.dto.response.FeedbackExportResult;
import com.siren.notificationservice.core.service.FeedbackExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notification/")
public class FeedbackExportController implements FeedbackExportControllerDoc {
    private final FeedbackExportService feedbackExportService;

    @GetMapping(value = "/internal/feedback-exports", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(defaultValue = "0") Long sinceId,
            @RequestParam(defaultValue = "5000") int limit) {
        FeedbackExportResult result = feedbackExportService.exportAsCsv(sinceId, limit);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("X-Last-Feedback-Log-Id", String.valueOf(result.lastFeedbackLogId()))
                .body(result.csv());
    }
}
