package com.siren.notificationservice.telegram.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 연동 여부 조회 응답 Dto
 * @param linked 이미 해당 봇에 연동되어 있으면 true
 */
@Schema(description = "텔레그램 연동 상태 조회 응답")
public record LinkStatusResponse(
        @Schema(description = "연동 여부") boolean linked) {
}
