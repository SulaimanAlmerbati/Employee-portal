package com.company.employeeportal.controller;

import com.company.employeeportal.dto.DashboardMetrics;
import com.company.employeeportal.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Autowired
    private ObjectMapper objectMapper;

    private DashboardMetrics testMetrics;

    @BeforeEach
    void setUp() {
        testMetrics = new DashboardMetrics();
        testMetrics.setTotalUsers(100L);
        testMetrics.setActiveUsers(95L);
        testMetrics.setTotalLeaveRequests(50L);
        testMetrics.setPendingLeaveRequests(10L);
        testMetrics.setApprovedLeaveRequests(35L);
        testMetrics.setRejectedLeaveRequests(5L);
        testMetrics.setLastUpdated(LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "IT_ADMIN")
    void getDashboardMetrics_Success() throws Exception {
        when(adminService.getDashboardMetrics()).thenReturn(testMetrics);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.activeUsers").value(95))
                .andExpect(jsonPath("$.totalLeaveRequests").value(50));

        verify(adminService).getDashboardMetrics();
    }

    @Test
    @WithMockUser(roles = "HR")
    void getDashboardMetrics_HRAccess_Success() throws Exception {
        when(adminService.getDashboardMetrics()).thenReturn(testMetrics);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());

        verify(adminService).getDashboardMetrics();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getDashboardMetrics_InsufficientRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());

        verify(adminService, never()).getDashboardMetrics();
    }

    @Test
    @WithMockUser(roles = "IT_ADMIN")
    void getUserStatistics_Success() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100L);
        stats.put("activeUsers", 95L);
        
        when(adminService.getUserStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/users/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.activeUsers").value(95));

        verify(adminService).getUserStatistics();
    }
}
