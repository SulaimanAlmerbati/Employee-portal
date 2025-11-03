package com.company.employeeportal.controller;

import com.company.employeeportal.dto.LeaveApprovalRequest;
import com.company.employeeportal.dto.LeaveRequest;
import com.company.employeeportal.dto.LeaveResponse;
import com.company.employeeportal.model.LeaveStatus;
import com.company.employeeportal.model.LeaveType;
import com.company.employeeportal.security.UserPrincipal;
import com.company.employeeportal.service.LeaveService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for LeaveController.
 */
@WebMvcTest(LeaveController.class)
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaveService leaveService;

    @Autowired
    private ObjectMapper objectMapper;

    private LeaveRequest leaveRequest;
    private LeaveResponse leaveResponse;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        // Create test leave request
        leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(7));
        leaveRequest.setEndDate(LocalDate.now().plusDays(9));
        leaveRequest.setReason("Family vacation");

        // Create test leave response
        leaveResponse = new LeaveResponse();
        leaveResponse.setId(1L);
        leaveResponse.setLeaveType(LeaveType.ANNUAL);
        leaveResponse.setStartDate(LocalDate.now().plusDays(7));
        leaveResponse.setEndDate(LocalDate.now().plusDays(9));
        leaveResponse.setReason("Family vacation");
        leaveResponse.setStatus(LeaveStatus.PENDING);
        leaveResponse.setCreatedAt(LocalDateTime.now());
        leaveResponse.setUpdatedAt(LocalDateTime.now());
        leaveResponse.setDurationInDays(3L);

        // Create test user principal
        userPrincipal = new UserPrincipal(1L, "john.doe@array.world", "password", Arrays.asList());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_ValidRequest_Success() throws Exception {
        // Arrange
        when(leaveService.createLeaveRequest(any(LeaveRequest.class), eq(1L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.leaveType").value("ANNUAL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.durationInDays").value(3));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_InvalidRequest_BadRequest() throws Exception {
        // Arrange
        leaveRequest.setReason(""); // Invalid empty reason

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_EndDateBeforeStartDate_BadRequest() throws Exception {
        // Arrange
        leaveRequest.setEndDate(LocalDate.now().plusDays(5)); // Before start date

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitLeaveRequest_Unauthenticated_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getMyLeaveRequests_Success() throws Exception {
        // Arrange
        List<LeaveResponse> leaveHistory = Arrays.asList(leaveResponse);
        when(leaveService.getUserLeaveHistory(1L)).thenReturn(leaveHistory);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/my-requests")
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].leaveType").value("ANNUAL"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getMyLeaveRequestsPaginated_Success() throws Exception {
        // Arrange
        Page<LeaveResponse> leavePage = new PageImpl<>(Arrays.asList(leaveResponse), 
                                                      PageRequest.of(0, 10), 1);
        when(leaveService.getUserLeaveHistory(eq(1L), any())).thenReturn(leavePage);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/my-requests/paginated")
                .with(user(userPrincipal))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getLeaveRequest_Success() throws Exception {
        // Arrange
        when(leaveService.getLeaveById(1L, 1L)).thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/1")
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.leaveType").value("ANNUAL"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getPendingLeaveRequests_Manager_Success() throws Exception {
        // Arrange
        List<LeaveResponse> pendingLeaves = Arrays.asList(leaveResponse);
        when(leaveService.getPendingLeaveRequests()).thenReturn(pendingLeaves);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/pending")
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getPendingLeaveRequests_Employee_Forbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/leaves/pending")
                .with(user(userPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getPendingLeaveRequestsPaginated_Success() throws Exception {
        // Arrange
        Page<LeaveResponse> pendingPage = new PageImpl<>(Arrays.asList(leaveResponse), 
                                                        PageRequest.of(0, 10), 1);
        when(leaveService.getPendingLeaveRequests(any())).thenReturn(pendingPage);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/pending/paginated")
                .with(user(userPrincipal))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void approveLeaveRequest_Success() throws Exception {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved for vacation");
        leaveResponse.setStatus(LeaveStatus.APPROVED);
        leaveResponse.setManagerComments("Approved for vacation");
        leaveResponse.setApprovedByName("Jane Manager");
        
        when(leaveService.approveLeaveRequest(eq(1L), any(LeaveApprovalRequest.class), eq(1L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/approve")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.managerComments").value("Approved for vacation"))
                .andExpect(jsonPath("$.approvedByName").value("Jane Manager"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void approveLeaveRequest_Employee_Forbidden() throws Exception {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved");

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/approve")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void rejectLeaveRequest_Success() throws Exception {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Insufficient staffing");
        leaveResponse.setStatus(LeaveStatus.REJECTED);
        leaveResponse.setManagerComments("Insufficient staffing");
        leaveResponse.setApprovedByName("Jane Manager");
        
        when(leaveService.rejectLeaveRequest(eq(1L), any(LeaveApprovalRequest.class), eq(1L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/reject")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.managerComments").value("Insufficient staffing"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void rejectLeaveRequest_Employee_Forbidden() throws Exception {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Rejected");

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/reject")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getApprovedLeavesForCalendar_Success() throws Exception {
        // Arrange
        leaveResponse.setStatus(LeaveStatus.APPROVED);
        List<LeaveResponse> approvedLeaves = Arrays.asList(leaveResponse);
        when(leaveService.getApprovedLeavesInDateRange(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(approvedLeaves);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/calendar")
                .with(user(userPrincipal))
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getLeaveStatistics_Success() throws Exception {
        // Arrange
        LeaveService.LeaveStatistics statistics = new LeaveService.LeaveStatistics(5, 3, 1, 1, 10);
        when(leaveService.getUserLeaveStatistics(1L, 2024)).thenReturn(statistics);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/statistics")
                .with(user(userPrincipal))
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(5))
                .andExpect(jsonPath("$.approvedRequests").value(3))
                .andExpect(jsonPath("$.pendingRequests").value(1))
                .andExpect(jsonPath("$.rejectedRequests").value(1))
                .andExpect(jsonPath("$.totalApprovedDays").value(10));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getLeaveStatistics_DefaultYear_Success() throws Exception {
        // Arrange
        LeaveService.LeaveStatistics statistics = new LeaveService.LeaveStatistics(3, 2, 1, 0, 7);
        when(leaveService.getUserLeaveStatistics(eq(1L), anyInt())).thenReturn(statistics);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/statistics")
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(3))
                .andExpect(jsonPath("$.approvedRequests").value(2));
    }

    @Test
    void healthCheck_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/leaves/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Leave service is running"));
    }

    // Additional tests for Requirements 3.1, 7.2, 7.5

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_MissingLeaveType_BadRequest() throws Exception {
        // Arrange - Test validation for required leave type (Requirement 3.1)
        leaveRequest.setLeaveType(null);

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_MissingStartDate_BadRequest() throws Exception {
        // Arrange - Test validation for required start date (Requirement 3.1)
        leaveRequest.setStartDate(null);

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_MissingEndDate_BadRequest() throws Exception {
        // Arrange - Test validation for required end date (Requirement 3.1)
        leaveRequest.setEndDate(null);

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_BlankReason_BadRequest() throws Exception {
        // Arrange - Test validation for required reason (Requirement 3.1)
        leaveRequest.setReason("   "); // Blank reason

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_ReasonTooLong_BadRequest() throws Exception {
        // Arrange - Test validation for reason length limit
        String longReason = "A".repeat(501); // Exceeds 500 character limit
        leaveRequest.setReason(longReason);

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void approveLeaveRequest_EmptyComments_Success() throws Exception {
        // Arrange - Test that approval comments are optional (Requirement 7.2)
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("");
        leaveResponse.setStatus(LeaveStatus.APPROVED);
        
        when(leaveService.approveLeaveRequest(eq(1L), any(LeaveApprovalRequest.class), eq(1L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/approve")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void rejectLeaveRequest_EmptyComments_BadRequest() throws Exception {
        // Arrange - Test that rejection requires comments (Requirement 7.2)
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("");

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/reject")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveLeaveRequest_Admin_Success() throws Exception {
        // Arrange - Test that admins can approve leave requests
        UserPrincipal adminPrincipal = new UserPrincipal(3L, "admin@array.world", "password", Arrays.asList());
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Admin approval");
        leaveResponse.setStatus(LeaveStatus.APPROVED);
        
        when(leaveService.approveLeaveRequest(eq(1L), any(LeaveApprovalRequest.class), eq(3L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/approve")
                .with(user(adminPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectLeaveRequest_Admin_Success() throws Exception {
        // Arrange - Test that admins can reject leave requests
        UserPrincipal adminPrincipal = new UserPrincipal(3L, "admin@array.world", "password", Arrays.asList());
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Admin rejection");
        leaveResponse.setStatus(LeaveStatus.REJECTED);
        
        when(leaveService.rejectLeaveRequest(eq(1L), any(LeaveApprovalRequest.class), eq(3L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/reject")
                .with(user(adminPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_StartDateInPast_BadRequest() throws Exception {
        // Arrange - Test validation for past start dates
        leaveRequest.setStartDate(LocalDate.now().minusDays(1));

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submitLeaveRequest_SingleDayLeave_Success() throws Exception {
        // Arrange - Test single day leave request
        LocalDate singleDay = LocalDate.now().plusDays(7);
        leaveRequest.setStartDate(singleDay);
        leaveRequest.setEndDate(singleDay);
        leaveResponse.setDurationInDays(1L);
        
        when(leaveService.createLeaveRequest(any(LeaveRequest.class), eq(1L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(post("/api/leaves/request")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leaveRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationInDays").value(1));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void approveLeaveRequest_NotificationSent_Success() throws Exception {
        // Arrange - Test that notification is sent on approval (Requirement 7.2)
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved for vacation");
        leaveResponse.setStatus(LeaveStatus.APPROVED);
        
        when(leaveService.approveLeaveRequest(eq(1L), any(LeaveApprovalRequest.class), eq(1L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/approve")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        
        // Note: Notification verification is handled in service layer tests
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void rejectLeaveRequest_NotificationSent_Success() throws Exception {
        // Arrange - Test that notification is sent on rejection (Requirement 7.2)
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Insufficient staffing");
        leaveResponse.setStatus(LeaveStatus.REJECTED);
        
        when(leaveService.rejectLeaveRequest(eq(1L), any(LeaveApprovalRequest.class), eq(1L)))
            .thenReturn(leaveResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/1/reject")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        
        // Note: Notification verification is handled in service layer tests
    }
}
