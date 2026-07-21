package com.siren.notificationservice.telegram.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프론트에 딥링크를 주기위한 Dto
 * @param deepLinkUrl 토큰까지 포함된 딥링크
 * @param expiresInSeconds "이 링크는 5분간 유효합니다" or 4:32 남음 같은 시간을 보여주기 위ㄴ
 */
@Schema(description = "사용자 딥링크 제공 데이터")
public record LinkTokenResponse(
        @Schema(description = "deepLinkUrl") String deepLinkUrl,
        @Schema(description = "uuid 만료시간") long expiresInSeconds) {
}
