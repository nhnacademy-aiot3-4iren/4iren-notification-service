package com.siren.notificationservice.telegram.controller;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.controller.webhook.doc.DeepLinkControllerDoc;
import com.siren.notificationservice.telegram.dto.response.LinkStatusResponse;
import com.siren.notificationservice.telegram.dto.response.LinkTokenResponse;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeepLinkController implements DeepLinkControllerDoc {
    private final TelegramLinkTokenService telegramLinkTokenService;


    /**
     * AdminBot에 연결하기 위한 DeepLink 제공
     * Gateway측에서 Admin role만 통과시켜야함
     */
    @PostMapping("/telegram/admin/link-token")
    public ResponseEntity<LinkTokenResponse> linkAdminToken(
            @RequestHeader("X-USER-ID") Long userId) {
        String deepLinkUrl = telegramLinkTokenService.getDeepLinkUrl(userId,  BotType.ADMIN_BOT);
        return ResponseEntity.ok().body(new LinkTokenResponse(deepLinkUrl, TelegramLinkTokenService.LINK_TOKEN_TTL.toSeconds()));
    }

    /**
     * MemberBot에 연결하기 위한 DeepLink 제공
     */
    @PostMapping("/telegram/member/link-token")
    public ResponseEntity<LinkTokenResponse> linkMemberToken(
            @RequestHeader("X-USER-ID") Long userId) {
        String deepLinkUrl = telegramLinkTokenService.getDeepLinkUrl(userId,  BotType.USER_BOT);
        return ResponseEntity.ok().body(new LinkTokenResponse(deepLinkUrl, TelegramLinkTokenService.LINK_TOKEN_TTL.toSeconds()));
    }

    /**
     * Admin 봇 연동 여부 조회. 딥링크 토큰 발급 전, 프론트가 "이미 연동되어 있습니다,
     * 재연동하시겠어요?" 확인 다이얼로그를 보여줄지 판단하는 데 쓴다.
     */
    @GetMapping("/telegram/admin/link-status")
    public ResponseEntity<LinkStatusResponse> getAdminLinkStatus(
            @RequestHeader("X-USER-ID") Long userId) {
        boolean linked = telegramLinkTokenService.isLinked(userId, BotType.ADMIN_BOT);
        return ResponseEntity.ok().body(new LinkStatusResponse(linked));
    }

    /**
     * Member 봇 연동 여부 조회. 딥링크 토큰 발급 전, 프론트가 "이미 연동되어 있습니다,
     * 재연동하시겠어요?" 확인 다이얼로그를 보여줄지 판단하는 데 쓴다 .
     */
    @GetMapping("/telegram/member/link-status")
    public ResponseEntity<LinkStatusResponse> getMemberLinkStatus(
            @RequestHeader("X-USER-ID") Long userId) {
        boolean linked = telegramLinkTokenService.isLinked(userId, BotType.USER_BOT);
        return ResponseEntity.ok().body(new LinkStatusResponse(linked));
    }
}
