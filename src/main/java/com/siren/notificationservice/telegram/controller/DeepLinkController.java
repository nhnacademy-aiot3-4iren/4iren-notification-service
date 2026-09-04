package com.siren.notificationservice.telegram.controller;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.core.security.RequireRole;
import com.siren.notificationservice.core.security.Role;
import com.siren.notificationservice.core.service.basic_service.TelegramSubscriptionService;
import com.siren.notificationservice.telegram.controller.webhook.doc.DeepLinkControllerDoc;
import com.siren.notificationservice.telegram.dto.LinkTokenData;
import com.siren.notificationservice.telegram.dto.response.LinkStatusResponse;
import com.siren.notificationservice.telegram.dto.response.LinkTokenResponse;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification/telegram")
public class DeepLinkController implements DeepLinkControllerDoc {
    private final TelegramLinkTokenService telegramLinkTokenService;
    private final TelegramSubscriptionService telegramSubscriptionService;


    /**
     * AdminBot에 연결하기 위한 DeepLink 제공
     * Gateway측에서 Admin role만 통과시켜야함
     */
    @RequireRole({Role.OWNER, Role.ADMIN})
    @PostMapping("/admin/link-token")
    public ResponseEntity<LinkTokenResponse> linkAdminToken(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestHeader("X-USER-ROLE")UserRole role) {
        String deepLinkUrl = telegramLinkTokenService.getDeepLinkUrl(new LinkTokenData(userId,role), BotType.ADMIN_BOT);
        return ResponseEntity.ok().body(new LinkTokenResponse(deepLinkUrl, TelegramLinkTokenService.LINK_TOKEN_TTL.toSeconds()));
    }

    /**
     * MemberBot에 연결하기 위한 DeepLink 제공
     */
    @RequireRole({Role.OWNER, Role.ADMIN, Role.NORMAL})
    @PostMapping("/member/link-token")
    public ResponseEntity<LinkTokenResponse> linkMemberToken(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestHeader("X-USER-ROLE")UserRole role) {
        String deepLinkUrl = telegramLinkTokenService.getDeepLinkUrl(new LinkTokenData(userId,role),  BotType.USER_BOT);
        return ResponseEntity.ok().body(new LinkTokenResponse(deepLinkUrl, TelegramLinkTokenService.LINK_TOKEN_TTL.toSeconds()));
    }

    /**
     * Admin 봇 연동 여부 조회. 딥링크 토큰 발급 전, 프론트가 "이미 연동되어 있습니다,
     * 재연동하시겠어요?" 확인 다이얼로그를 보여줄지 판단하는 데 쓴다.
     */
    @RequireRole({Role.OWNER, Role.ADMIN})
    @GetMapping("/admin/link-status")
    public ResponseEntity<LinkStatusResponse> getAdminLinkStatus(
            @RequestHeader("X-USER-ID") Long userId) {
        boolean linked = telegramSubscriptionService.isLinked(userId, BotType.ADMIN_BOT);
        return ResponseEntity.ok().body(new LinkStatusResponse(linked));
    }

    /**
     * Member 봇 연동 여부 조회. 딥링크 토큰 발급 전, 프론트가 "이미 연동되어 있습니다,
     * 재연동하시겠어요?" 확인 다이얼로그를 보여줄지 판단하는 데 쓴다 .
     */
    @RequireRole({Role.OWNER, Role.ADMIN, Role.NORMAL})
    @GetMapping("/member/link-status")
    public ResponseEntity<LinkStatusResponse> getMemberLinkStatus(
            @RequestHeader("X-USER-ID") Long userId) {
        boolean linked = telegramSubscriptionService.isLinked(userId, BotType.USER_BOT);
        return ResponseEntity.ok().body(new LinkStatusResponse(linked));
    }
}
