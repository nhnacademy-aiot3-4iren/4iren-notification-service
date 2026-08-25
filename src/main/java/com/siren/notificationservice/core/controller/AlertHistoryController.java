package com.siren.notificationservice.core.controller;

import com.siren.notificationservice.core.controller.doc.AlertHistoryControllerDoc;
import com.siren.notificationservice.core.dto.request.AlertHistorySearchCondition;
import com.siren.notificationservice.core.dto.response.AlertHistoryFilterOptionsResponse;
import com.siren.notificationservice.core.dto.response.AlertHistoryResponse;
import com.siren.notificationservice.core.dto.response.PageResponse;
import com.siren.notificationservice.core.entity.domain.AlertType;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.security.RequireRole;
import com.siren.notificationservice.core.security.Role;
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
@RequireRole({Role.OWNER, Role.ADMIN})
public class AlertHistoryController implements AlertHistoryControllerDoc {
    private final AlertHistoryService alertHistoryService;

    @GetMapping
    public ResponseEntity<PageResponse<AlertHistoryResponse>> getAllAlertHistory(
            @RequestHeader("X-USER-ID") Long userId,
            AlertHistorySearchCondition condition,
            @PageableDefault(size = 20, sort = "sendAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        validateFilter(condition); // 트랜잭션 진입 전에 enum 값 검증 — 잘못된 값이면 여기서 IllegalArgumentException(→ 400)
        return ResponseEntity.ok().body(PageResponse.from(alertHistoryService.getAlertHistoryByUserId(userId,condition, pageable)));
    }

    /** botType/alertType 문자열이 유효 enum인지 트랜잭션 밖(컨트롤러)에서 미리 검증한다. 잘못된 값이면 IllegalArgumentException → GlobalExceptionHandler에서 400. */
    private void validateFilter(AlertHistorySearchCondition condition) {
        if (condition.botType() != null) BotType.valueOf(condition.botType());
        if (condition.alertType() != null) AlertType.valueOf(condition.alertType());
    }

    @GetMapping("/{alert-history-id}")
    public ResponseEntity<AlertHistoryResponse> getAlertHistoryById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("alert-history-id")  Long alertHistoryId
    ){
        return ResponseEntity.ok().body(alertHistoryService.getAlertHistoryById(alertHistoryId, userId));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<AlertHistoryFilterOptionsResponse>  getAlertHistoryFilterOptions(
            @RequestHeader("X-USER-ID") Long userId
    ){
        return ResponseEntity.ok(alertHistoryService.getFilterOptions(userId));
    }
}
