package com.siren.notificationservice.core.controller.doc;

import com.siren.notificationservice.core.dto.response.AlertHistoryResponse;
import com.siren.notificationservice.core.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "알림 이력 API", description = "유저에게 발송된 알림 이력 조회 API")
public interface AlertHistoryControllerDoc {

    /**
     * 로그인한 유저에게 발송된 알림 이력 목록 조회.
     */
    @Operation(
            summary = "내 알림 이력 목록 조회",
            description = "로그인한 유저(X-USER-ID)에게 발송된 알림 이력을 최신순(sendAt desc)으로 페이지 조회한다. "
                    + "공동 관리자여도 각자 자기에게 온 이력만 보인다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<PageResponse<AlertHistoryResponse>> getAllAlertHistory(
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId,
            Pageable pageable
    );

    /**
     * 알림 이력 단건 조회.
     */
    @Operation(
            summary = "알림 이력 단건 조회",
            description = "alertHistoryId로 알림 이력 한 건을 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "해당 id의 알림 이력이 없거나 요청자 소유가 아님")
    ResponseEntity<AlertHistoryResponse> getAlertHistoryById(
            @Parameter(hidden = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(description = "알림 이력 id", required = true)
            @PathVariable("alert-history-id") Long alertHistoryId
    );
}
