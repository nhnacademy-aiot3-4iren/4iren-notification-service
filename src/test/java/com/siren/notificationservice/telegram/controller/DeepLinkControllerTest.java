package com.siren.notificationservice.telegram.controller;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.core.entity.domain.UserRole;
import com.siren.notificationservice.telegram.dto.LinkTokenData;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeepLinkController.class)
class DeepLinkControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TelegramLinkTokenService telegramLinkTokenService;

    @Test
    void linkAdminTokenReturnsDeepLinkUrl() throws Exception {
        when(telegramLinkTokenService.getDeepLinkUrl(new LinkTokenData(1L, UserRole.ADMIN), BotType.ADMIN_BOT))
                .thenReturn("https://t.me/admin_bot?start=abc");

        mockMvc.perform(post("/telegram/admin/link-token").header("X-USER-ID", "1").header("X-USER-ROLE", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepLinkUrl").value("https://t.me/admin_bot?start=abc"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    void linkMemberTokenReturnsDeepLinkUrl() throws Exception {
        when(telegramLinkTokenService.getDeepLinkUrl(new LinkTokenData(1L, UserRole.NORMAL), BotType.USER_BOT))
                .thenReturn("https://t.me/member_bot?start=abc");

        mockMvc.perform(post("/telegram/member/link-token").header("X-USER-ID", "1").header("X-USER-ROLE", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepLinkUrl").value("https://t.me/member_bot?start=abc"));
    }

    @Test
    void getAdminLinkStatusReturnsLinkedTrue() throws Exception {
        when(telegramLinkTokenService.isLinked(1L, BotType.ADMIN_BOT)).thenReturn(true);

        mockMvc.perform(get("/telegram/admin/link-status").header("X-USER-ID", "1").header("X-USER-ROLE", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true));
    }

    @Test
    void getMemberLinkStatusReturnsLinkedFalse() throws Exception {
        when(telegramLinkTokenService.isLinked(1L, BotType.USER_BOT)).thenReturn(false);

        mockMvc.perform(get("/telegram/member/link-status").header("X-USER-ID", "1").header("X-USER-ROLE", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false));
    }

    // --- 역할 경계: admin 엔드포인트는 @RequireRole({OWNER, ADMIN}) ---

    @Test
    void linkAdminTokenForbiddenForNormalRole() throws Exception {
        mockMvc.perform(post("/telegram/admin/link-token").header("X-USER-ID", "1").header("X-USER-ROLE", "NORMAL"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(telegramLinkTokenService);
    }

    @Test
    void getAdminLinkStatusForbiddenForNormalRole() throws Exception {
        mockMvc.perform(get("/telegram/admin/link-status").header("X-USER-ID", "1").header("X-USER-ROLE", "NORMAL"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(telegramLinkTokenService);
    }

    @Test
    void adminTokenForbiddenWhenRoleHeaderMissing() throws Exception {
        mockMvc.perform(post("/telegram/admin/link-token").header("X-USER-ID", "1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(telegramLinkTokenService);
    }

    // --- 역할 경계: member 엔드포인트는 @RequireRole({OWNER, ADMIN, NORMAL}) → 상위 role도 통과 ---

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN"})
    void linkMemberTokenAllowedForElevatedRoles(String role) throws Exception {
        when(telegramLinkTokenService.getDeepLinkUrl(new LinkTokenData(1L, UserRole.valueOf(role)), BotType.USER_BOT))
                .thenReturn("https://t.me/member_bot?start=abc");

        mockMvc.perform(post("/telegram/member/link-token").header("X-USER-ID", "1").header("X-USER-ROLE", role))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepLinkUrl").value("https://t.me/member_bot?start=abc"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN"})
    void getMemberLinkStatusAllowedForElevatedRoles(String role) throws Exception {
        when(telegramLinkTokenService.isLinked(1L, BotType.USER_BOT)).thenReturn(true);

        mockMvc.perform(get("/telegram/member/link-status").header("X-USER-ID", "1").header("X-USER-ROLE", role))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true));
    }
}
