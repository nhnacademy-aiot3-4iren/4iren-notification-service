package com.siren.notificationservice.telegram.controller;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.dto.response.LinkStatusResponse;
import com.siren.notificationservice.telegram.dto.response.LinkTokenResponse;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "딥링크 제공 API",description = "역할별로 차별화된 딥링크 Rest API 제공")
public class DeepLinkController {
    private final TelegramLinkTokenService telegramLinkTokenService;


    /**
     * AdminBot에 연결하기 위한 DeepLink 제공
     * Gateway측에서 Admin role만 통과시켜야함
     */
    @Operation(
            summary = "Admin 봇 연동 딥링크 발급",
            description = "관리자 계정을 Admin 봇에 연동하기 위한 1회용 딥링크를 발급한다. "
                    + "Gateway가 Admin role만 통과시킨다는 전제로 동작하며, 토큰은 "
                    + TelegramLinkTokenService.LINK_TOKEN_TTL_MINUTES + "분 후 만료된다."
    )
    @ApiResponse(responseCode = "200", description = "딥링크 발급 성공")
    @PostMapping("/telegram/admin/link-token")
    public ResponseEntity<LinkTokenResponse> linkAdminToken(
            @Parameter(description = "Gateway가 JWT 검증 후 전달하는 유저 id", required = true)
            @RequestHeader("X-USER-ID") Long userId) {
        String deepLinkUrl = telegramLinkTokenService.getDeepLinkUrl(userId,  BotType.ADMIN_BOT);
        return ResponseEntity.ok().body(new LinkTokenResponse(deepLinkUrl, TelegramLinkTokenService.LINK_TOKEN_TTL.toSeconds()));
    }

    /**
     * MemberBot에 연결하기 위한 DeepLink 제공
     */
    @Operation(
            summary = "Member 봇 연동 딥링크 발급",
            description = "로그인한 유저를 Member 봇에 연동하기 위한 1회용 딥링크를 발급한다. 토큰은 "
                    + TelegramLinkTokenService.LINK_TOKEN_TTL_MINUTES + "분 후 만료된다."
    )
    @ApiResponse(responseCode = "200", description = "딥링크 발급 성공")
    @PostMapping("/telegram/member/link-token")
    public ResponseEntity<LinkTokenResponse> linkMemberToken(
            @Parameter(description = "Gateway가 JWT 검증 후 전달하는 유저 id", required = true)
            @RequestHeader("X-USER-ID") Long userId) {
        String deepLinkUrl = telegramLinkTokenService.getDeepLinkUrl(userId,  BotType.USER_BOT);
        return ResponseEntity.ok().body(new LinkTokenResponse(deepLinkUrl, TelegramLinkTokenService.LINK_TOKEN_TTL.toSeconds()));
    }

    /**
     * Admin 봇 연동 여부 조회. 딥링크 토큰 발급 전, 프론트가 "이미 연동되어 있습니다,
     * 재연동하시겠어요?" 확인 다이얼로그를 보여줄지 판단하는 데 쓴다.
     */
    @Operation(
            summary = "Admin 봇 연동 여부 조회",
            description = "관리자 계정이 Admin 봇에 이미 연동돼 있는지 조회한다. "
                    + "딥링크 토큰을 발급하기 전에 재연동 확인 다이얼로그를 띄울지 판단하는 데 쓴다."
    )
    @ApiResponse(responseCode = "200", description = "연동 상태 조회 성공")
    @GetMapping("/telegram/admin/link-status")
    public ResponseEntity<LinkStatusResponse> getAdminLinkStatus(
            @Parameter(description = "Gateway가 JWT 검증 후 전달하는 유저 id", required = true)
            @RequestHeader("X-USER-ID") Long userId) {
        boolean linked = telegramLinkTokenService.isLinked(userId, BotType.ADMIN_BOT);
        return ResponseEntity.ok().body(new LinkStatusResponse(linked));
    }

    /**
     * Member 봇 연동 여부 조회. 딥링크 토큰 발급 전, 프론트가 "이미 연동되어 있습니다,
     * 재연동하시겠어요?" 확인 다이얼로그를 보여줄지 판단하는 데 쓴다 .
     */
    @Operation(
            summary = "Member 봇 연동 여부 조회",
            description = "로그인한 유저가 Member 봇에 이미 연동돼 있는지 조회한다. "
                    + "딥링크 토큰을 발급하기 전에 재연동 확인 다이얼로그를 띄울지 판단하는 데 쓴다."
    )
    @ApiResponse(responseCode = "200", description = "연동 상태 조회 성공")
    @GetMapping("/telegram/member/link-status")
    public ResponseEntity<LinkStatusResponse> getMemberLinkStatus(
            @Parameter(description = "Gateway가 JWT 검증 후 전달하는 유저 id", required = true)
            @RequestHeader("X-USER-ID") Long userId) {
        boolean linked = telegramLinkTokenService.isLinked(userId, BotType.USER_BOT);
        return ResponseEntity.ok().body(new LinkStatusResponse(linked));
    }
}
