package com.siren.notificationservice.telegram.controller;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.service.TelegramLinkTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
        when(telegramLinkTokenService.getDeepLinkUrl(1L, BotType.ADMIN_BOT))
                .thenReturn("https://t.me/admin_bot?start=abc");

        mockMvc.perform(post("/telegram/admin/link-token").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepLinkUrl").value("https://t.me/admin_bot?start=abc"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    void linkMemberTokenReturnsDeepLinkUrl() throws Exception {
        when(telegramLinkTokenService.getDeepLinkUrl(1L, BotType.USER_BOT))
                .thenReturn("https://t.me/member_bot?start=abc");

        mockMvc.perform(post("/telegram/member/link-token").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepLinkUrl").value("https://t.me/member_bot?start=abc"));
    }

    @Test
    void getAdminLinkStatusReturnsLinkedTrue() throws Exception {
        when(telegramLinkTokenService.isLinked(1L, BotType.ADMIN_BOT)).thenReturn(true);

        mockMvc.perform(get("/telegram/admin/link-status").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true));
    }

    @Test
    void getMemberLinkStatusReturnsLinkedFalse() throws Exception {
        when(telegramLinkTokenService.isLinked(1L, BotType.USER_BOT)).thenReturn(false);

        mockMvc.perform(get("/telegram/member/link-status").header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false));
    }
}
