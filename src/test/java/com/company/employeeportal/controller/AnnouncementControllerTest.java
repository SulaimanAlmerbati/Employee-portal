package com.company.employeeportal.controller;

import com.company.employeeportal.dto.AnnouncementRequest;
import com.company.employeeportal.dto.AnnouncementResponse;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.security.UserPrincipal;
import com.company.employeeportal.service.AnnouncementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AnnouncementController.
 */
@WebMvcTest(AnnouncementController.class)
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnnouncementService announcementService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal employeePrincipal;
    private UserPrincipal adminPrincipal;
    private AnnouncementResponse testAnnouncementResponse;
    private AnnouncementRequest testAnnouncementRequest;

    @BeforeEach
    void setUp() {
        // Create test user principals
        employeePrincipal = new UserPrincipal(2L, "employee@company.com", "password", Role.EMPLOYEE);
        adminPrincipal = new UserPrincipal(1L, "admin@company.com", "password", Role.ADMIN);

        // Create test announcement response
        testAnnouncementResponse = new AnnouncementResponse(
                1L, "Test Announcement", "Test content", "Admin User", 1L, 
                true, LocalDateTime.now(), LocalDateTime.now());

        // Create test announcement request
        testAnnouncementRequest = new AnnouncementRequest("New Announcement", "New content");
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getActiveAnnouncements_ShouldReturnPagedResults() throws Exception {
        // Arrange
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        Page<AnnouncementResponse> page = new PageImpl<>(announcements, PageRequest.of(0, 10), 1);
        when(announcementService.getActiveAnnouncements(any(), eq(2L))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/announcements")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Announcement"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(announcementService).getActiveAnnouncements(any(), eq(2L));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getRecentAnnouncements_ShouldReturnRecentList() throws Exception {
        // Arrange
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        when(announcementService.getRecentAnnouncements(2L)).thenReturn(announcements);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/recent")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Test Announcement"));

        verify(announcementService).getRecentAnnouncements(2L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAnnouncementById_ShouldReturnAnnouncement() throws Exception {
        // Arrange
        when(announcementService.getAnnouncementById(1L, 2L)).thenReturn(testAnnouncementResponse);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/1")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Announcement"))
                .andExpect(jsonPath("$.content").value("Test content"));

        verify(announcementService).getAnnouncementById(1L, 2L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void searchAnnouncements_ShouldReturnSearchResults() throws Exception {
        // Arrange
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        Page<AnnouncementResponse> page = new PageImpl<>(announcements, PageRequest.of(0, 10), 1);
        when(announcementService.searchAnnouncements(eq("test"), eq(true), any(), eq(2L))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/search")
                .param("query", "test")
                .param("activeOnly", "true")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Announcement"));

        verify(announcementService).searchAnnouncements(eq("test"), eq(true), any(), eq(2L));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void markAnnouncementAsRead_ShouldReturnOk() throws Exception {
        // Arrange
        doNothing().when(announcementService).markAnnouncementAsRead(1L, 2L);

        // Act & Assert
        mockMvc.perform(post("/api/announcements/1/mark-read")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk());

        verify(announcementService).markAnnouncementAsRead(1L, 2L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getReadStatus_ShouldReturnReadStatus() throws Exception {
        // Arrange
        when(announcementService.isAnnouncementReadByUser(1L, 2L)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/1/read-status")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(announcementService).isAnnouncementReadByUser(1L, 2L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAnnouncements_WithAdminRole_ShouldReturnAllAnnouncements() throws Exception {
        // Arrange
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        Page<AnnouncementResponse> page = new PageImpl<>(announcements, PageRequest.of(0, 10), 1);
        when(announcementService.getAllAnnouncements(any(), eq(1L))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/admin/all")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Announcement"));

        verify(announcementService).getAllAnnouncements(any(), eq(1L));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAllAnnouncements_WithEmployeeRole_ShouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/announcements/admin/all")
                .with(user(employeePrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).getAllAnnouncements(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAnnouncement_WithAdminRole_ShouldCreateSuccessfully() throws Exception {
        // Arrange
        when(announcementService.createAnnouncement(any(AnnouncementRequest.class), eq(1L)))
                .thenReturn(testAnnouncementResponse);

        // Act & Assert
        mockMvc.perform(post("/api/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncementRequest))
                .with(user(adminPrincipal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Announcement"));

        verify(announcementService).createAnnouncement(any(AnnouncementRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createAnnouncement_WithEmployeeRole_ShouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncementRequest))
                .with(user(employeePrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).createAnnouncement(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAnnouncement_WithAdminRole_ShouldUpdateSuccessfully() throws Exception {
        // Arrange
        when(announcementService.updateAnnouncement(eq(1L), any(AnnouncementRequest.class), eq(1L)))
                .thenReturn(testAnnouncementResponse);

        // Act & Assert
        mockMvc.perform(put("/api/announcements/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncementRequest))
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Announcement"));

        verify(announcementService).updateAnnouncement(eq(1L), any(AnnouncementRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateAnnouncement_WithEmployeeRole_ShouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/announcements/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncementRequest))
                .with(user(employeePrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).updateAnnouncement(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateAnnouncement_WithAdminRole_ShouldDeactivateSuccessfully() throws Exception {
        // Arrange
        doNothing().when(announcementService).deactivateAnnouncement(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/announcements/1")
                .with(user(adminPrincipal)))
                .andExpect(status().isNoContent());

        verify(announcementService).deactivateAnnouncement(1L, 1L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deactivateAnnouncement_WithEmployeeRole_ShouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/announcements/1")
                .with(user(employeePrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).deactivateAnnouncement(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reactivateAnnouncement_WithAdminRole_ShouldReactivateSuccessfully() throws Exception {
        // Arrange
        doNothing().when(announcementService).reactivateAnnouncement(1L, 1L);

        // Act & Assert
        mockMvc.perform(post("/api/announcements/1/reactivate")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk());

        verify(announcementService).reactivateAnnouncement(1L, 1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAnnouncementStatistics_WithAdminRole_ShouldReturnStatistics() throws Exception {
        // Arrange
        AnnouncementService.AnnouncementStatistics stats = 
                new AnnouncementService.AnnouncementStatistics(10L, 8L);
        when(announcementService.getAnnouncementStatistics()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/admin/statistics")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnnouncements").value(10))
                .andExpect(jsonPath("$.activeAnnouncements").value(8))
                .andExpect(jsonPath("$.inactiveAnnouncements").value(2));

        verify(announcementService).getAnnouncementStatistics();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAnnouncementStatistics_WithEmployeeRole_ShouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/announcements/admin/statistics")
                .with(user(employeePrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).getAnnouncementStatistics();
    }

    @Test
    void createAnnouncement_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AnnouncementRequest invalidRequest = new AnnouncementRequest("", ""); // Empty title and content

        // Act & Assert
        mockMvc.perform(post("/api/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(user(adminPrincipal)))
                .andExpect(status().isBadRequest());

        verify(announcementService, never()).createAnnouncement(any(), any());
    }

    @Test
    void updateAnnouncement_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AnnouncementRequest invalidRequest = new AnnouncementRequest("", ""); // Empty title and content

        // Act & Assert
        mockMvc.perform(put("/api/announcements/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(user(adminPrincipal)))
                .andExpect(status().isBadRequest());

        verify(announcementService, never()).updateAnnouncement(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getActiveAnnouncements_WithPaginationAndSorting_ShouldReturnSortedResults() throws Exception {
        // Arrange
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        Page<AnnouncementResponse> page = new PageImpl<>(announcements, PageRequest.of(0, 5), 1);
        when(announcementService.getActiveAnnouncements(any(), eq(2L))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/announcements")
                .param("page", "0")
                .param("size", "5")
                .param("sort", "createdAt,desc")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(announcementService).getActiveAnnouncements(any(), eq(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAnnouncements_WithPaginationParameters_ShouldRespectPagination() throws Exception {
        // Arrange
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        Page<AnnouncementResponse> page = new PageImpl<>(announcements, PageRequest.of(1, 10), 15);
        when(announcementService.getAllAnnouncements(any(), eq(1L))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/admin/all")
                .param("page", "1")
                .param("size", "10")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalElements").value(15));

        verify(announcementService).getAllAnnouncements(any(), eq(1L));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createAnnouncement_WithManagerRole_ShouldReturnForbidden() throws Exception {
        // Arrange
        UserPrincipal managerPrincipal = new UserPrincipal(3L, "manager@company.com", "password", Role.MANAGER);

        // Act & Assert
        mockMvc.perform(post("/api/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncementRequest))
                .with(user(managerPrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).createAnnouncement(any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateAnnouncement_WithManagerRole_ShouldReturnForbidden() throws Exception {
        // Arrange
        UserPrincipal managerPrincipal = new UserPrincipal(3L, "manager@company.com", "password", Role.MANAGER);

        // Act & Assert
        mockMvc.perform(put("/api/announcements/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncementRequest))
                .with(user(managerPrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).updateAnnouncement(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deactivateAnnouncement_WithManagerRole_ShouldReturnForbidden() throws Exception {
        // Arrange
        UserPrincipal managerPrincipal = new UserPrincipal(3L, "manager@company.com", "password", Role.MANAGER);

        // Act & Assert
        mockMvc.perform(delete("/api/announcements/1")
                .with(user(managerPrincipal)))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).deactivateAnnouncement(any(), any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void searchAnnouncements_WithPaginationAndSorting_ShouldReturnPaginatedResults() throws Exception {
        // Arrange
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        Page<AnnouncementResponse> page = new PageImpl<>(announcements, PageRequest.of(0, 20), 1);
        when(announcementService.searchAnnouncements(eq("test"), eq(true), any(), eq(2L))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/announcements/search")
                .param("query", "test")
                .param("activeOnly", "true")
                .param("page", "0")
                .param("size", "20")
                .param("sort", "title,asc")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));

        verify(announcementService).searchAnnouncements(eq("test"), eq(true), any(), eq(2L));
    }
}