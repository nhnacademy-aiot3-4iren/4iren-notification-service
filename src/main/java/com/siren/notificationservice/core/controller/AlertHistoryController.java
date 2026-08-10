package com.siren.notificationservice.core.controller;

import com.siren.notificationservice.core.controller.doc.AlertHistoryControllerDoc;
import com.siren.notificationservice.core.dto.response.AlertHistoryResponse;
import com.siren.notificationservice.core.dto.response.PageResponse;
import com.siren.notificationservice.core.service.basic_service.AlertHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification/alert-histories")
@RequiredArgsConstructor
public class AlertHistoryController implements AlertHistoryControllerDoc {
    private final AlertHistoryService alertHistoryService;

    @GetMapping
    public ResponseEntity<PageResponse<AlertHistoryResponse>> getAllAlertHistory(
            @RequestHeader("X-USER-ID") Long userId,
            @PageableDefault(size = 20, sort = "sendAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        return ResponseEntity.ok().body(PageResponse.from(alertHistoryService.getAlertHistoryByUserId(userId, pageable)));
    }

    @GetMapping("/{alert-history-id}")
    public ResponseEntity<AlertHistoryResponse> getAlertHistoryById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("alert-history-id")  Long alertHistoryId
    ){
        return ResponseEntity.ok().body(alertHistoryService.getAlertHistoryById(alertHistoryId, userId));
    }
}
