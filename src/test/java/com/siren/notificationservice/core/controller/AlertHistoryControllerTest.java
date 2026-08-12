package com.siren.notificationservice.core.controller;

import com.siren.notificationservice.core.dto.request.AlertHistorySearchCondition;
import com.siren.notificationservice.core.dto.response.AlertHistoryFilterOptionsResponse;
import com.siren.notificationservice.core.dto.response.AlertHistoryResponse;
import com.siren.notificationservice.core.exception.NotFoundAlertHistoryException;
import com.siren.notificationservice.core.service.basic_service.AlertHistoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertHistoryController.class)
class AlertHistoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AlertHistoryService alertHistoryService;

    private static final long USER_ID = 42L;
    private static final String USER_ROLE = "ADMIN";

    private AlertHistoryResponse sampleResponse() {
        return new AlertHistoryResponse(
                7L,
                101L,
                "MEMBER",
                "SENSOR_ANOMALY",
                "온도 이상 감지",
                LocalDateTime.of(2026, 8, 11, 9, 30)
        );
    }

    // --- GET /api/notification/alert-histories ---

    @Test
    void getAllAlertHistory_returnsPagedResponse() throws Exception {
        Page<AlertHistoryResponse> page = new PageImpl<>(
                List.of(sampleResponse()),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "sendAt")),
                1);
        when(alertHistoryService.getAlertHistoryByUserId(eq(USER_ID), any(AlertHistorySearchCondition.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/notification/alert-histories")
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].alertHistoryId").value(7))
                .andExpect(jsonPath("$.content[0].roomId").value(101))
                .andExpect(jsonPath("$.content[0].botType").value("MEMBER"))
                .andExpect(jsonPath("$.content[0].alertType").value("SENSOR_ANOMALY"))
                .andExpect(jsonPath("$.content[0].message").value("온도 이상 감지"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getAllAlertHistory_passesUserIdAndDefaultPageable() throws Exception {
        when(alertHistoryService.getAlertHistoryByUserId(eq(USER_ID), any(AlertHistorySearchCondition.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/notification/alert-histories")
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertHistoryService).getAlertHistoryByUserId(eq(USER_ID), any(AlertHistorySearchCondition.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getPageNumber()).isZero();
        Sort.Order sendAtOrder = pageable.getSort().getOrderFor("sendAt");
        assertThat(sendAtOrder).isNotNull();
        assertThat(sendAtOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllAlertHistory_honorsRequestedPaging() throws Exception {
        when(alertHistoryService.getAlertHistoryByUserId(eq(USER_ID), any(AlertHistorySearchCondition.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/notification/alert-histories")
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE)
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertHistoryService).getAlertHistoryByUserId(eq(USER_ID), any(AlertHistorySearchCondition.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    void getAllAlertHistory_bindsSearchCondition() throws Exception {
        when(alertHistoryService.getAlertHistoryByUserId(eq(USER_ID), any(AlertHistorySearchCondition.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/notification/alert-histories")
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE)
                        .param("alertType", "SENSOR_ANOMALY")
                        .param("roomId", "101"))
                .andExpect(status().isOk());

        ArgumentCaptor<AlertHistorySearchCondition> condCaptor = ArgumentCaptor.forClass(AlertHistorySearchCondition.class);
        verify(alertHistoryService).getAlertHistoryByUserId(eq(USER_ID), condCaptor.capture(), any(Pageable.class));

        AlertHistorySearchCondition cond = condCaptor.getValue();
        assertThat(cond.alertType().name()).isEqualTo("SENSOR_ANOMALY");
        assertThat(cond.roomId()).isEqualTo(101L);
    }

    // --- GET /api/notification/alert-histories/filter-options ---

    @Test
    void getFilterOptions_returnsConnectedBotsAndReceivedAlertTypes() throws Exception {
        when(alertHistoryService.getFilterOptions(USER_ID))
                .thenReturn(new AlertHistoryFilterOptionsResponse(List.of("ADMIN_BOT", "USER_BOT"), List.of("SENSOR_ANOMALY")));

        mockMvc.perform(get("/api/notification/alert-histories/filter-options")
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botTypeList[0]").value("ADMIN_BOT"))
                .andExpect(jsonPath("$.botTypeList[1]").value("USER_BOT"))
                .andExpect(jsonPath("$.alertTypeList[0]").value("SENSOR_ANOMALY"));
    }

    // --- GET /api/notification/alert-histories/{alert-history-id} ---

    @Test
    void getAlertHistoryById_returnsResponse() throws Exception {
        when(alertHistoryService.getAlertHistoryById(7L, USER_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/notification/alert-histories/{id}", 7L)
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertHistoryId").value(7))
                .andExpect(jsonPath("$.roomId").value(101))
                .andExpect(jsonPath("$.botType").value("MEMBER"))
                .andExpect(jsonPath("$.alertType").value("SENSOR_ANOMALY"))
                .andExpect(jsonPath("$.message").value("온도 이상 감지"));
    }

    @Test
    void getAlertHistoryById_passesPathVariableAndUserId() throws Exception {
        when(alertHistoryService.getAlertHistoryById(7L, USER_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/notification/alert-histories/{id}", 7L)
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE))
                .andExpect(status().isOk());

        verify(alertHistoryService).getAlertHistoryById(7L, USER_ID);
    }

    // --- error mapping (GlobalExceptionHandler) ---

    @Test
    void getAlertHistoryById_whenNotOwnedOrMissing_returns404() throws Exception {
        when(alertHistoryService.getAlertHistoryById(7L, USER_ID))
                .thenThrow(new NotFoundAlertHistoryException(7L));

        mockMvc.perform(get("/api/notification/alert-histories/{id}", 7L)
                        .header("X-USER-ID", USER_ID)
                        .header("X-USER-ROLE", USER_ROLE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAllAlertHistory_whenUserIdHeaderMissing_returns400() throws Exception {
        mockMvc.perform(get("/api/notification/alert-histories")
                        .header("X-USER-ROLE", USER_ROLE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
