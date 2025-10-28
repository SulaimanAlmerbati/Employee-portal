package com.company.employeeportal.service;

import com.company.employeeportal.dto.LeaveApprovalRequest;
import com.company.employeeportal.dto.LeaveRequest;
import com.company.employeeportal.dto.LeaveResponse;
import com.company.employeeportal.exception.InvalidLeaveRequestException;
import com.company.employeeportal.exception.LeaveApprovalException;
import com.company.employeeportal.exception.LeaveNotFoundException;
import com.company.employeeportal.exception.UnauthorizedAccessException;
import com.company.employeeportal.model.*;
import com.company.employeeportal.repository.LeaveRepository;
import com.company.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LeaveService.
 */
@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LeaveService leaveService;

    private User employee;
    private User manager;
    private Leave leave;
    private LeaveRequest leaveRequest;

    @BeforeEach
    void setUp() {
        // Create test employee
        employee = new User();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setEmail("john.doe@company.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setDepartment("IT");

        // Create test manager
        manager = new User();
        manager.setId(2L);
        manager.setName("Jane Manager");
        manager.setEmail("jane.manager@company.com");
        manager.setRole(Role.MANAGER);
        manager.setDepartment("IT");

        // Create test leave request
        leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(7));
        leaveRequest.setEndDate(LocalDate.now().plusDays(9));
        leaveRequest.setReason("Family vacation");

        // Create test leave entity
        leave = new Leave();
        leave.setId(1L);
        leave.setUser(employee);
        leave.setLeaveType(LeaveType.ANNUAL);
        leave.setStartDate(LocalDate.now().plusDays(7));
        leave.setEndDate(LocalDate.now().plusDays(9));
        leave.setReason("Family vacation");
        leave.setStatus(LeaveStatus.PENDING);
        leave.setCreatedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createLeaveRequest_ValidRequest_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUserAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());
        when(leaveRepository.save(any(Leave.class))).thenReturn(leave);

        // Act
        LeaveResponse response = leaveService.createLeaveRequest(leaveRequest, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(LeaveType.ANNUAL, response.getLeaveType());
        assertEquals(LeaveStatus.PENDING, response.getStatus());
        assertEquals(3L, response.getDurationInDays());

        verify(userRepository).findById(1L);
        verify(leaveRepository).save(any(Leave.class));
        verify(notificationService).sendLeaveRequestSubmittedNotification(any(Leave.class));
        verify(notificationService).sendNewLeaveRequestNotificationToManagers(any(Leave.class));
    }

    @Test
    void createLeaveRequest_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));

        verify(userRepository).findById(1L);
        verifyNoInteractions(leaveRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void createLeaveRequest_EndDateBeforeStartDate_ThrowsException() {
        // Arrange
        leaveRequest.setEndDate(LocalDate.now().plusDays(5));
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));

        verify(userRepository).findById(1L);
        verifyNoInteractions(leaveRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void createLeaveRequest_StartDateInPast_ThrowsException() {
        // Arrange
        leaveRequest.setStartDate(LocalDate.now().minusDays(1));
        leaveRequest.setEndDate(LocalDate.now().plusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));

        verify(userRepository).findById(1L);
        verifyNoInteractions(leaveRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void createLeaveRequest_OverlappingLeave_ThrowsException() {
        // Arrange
        Leave existingLeave = new Leave();
        existingLeave.setStatus(LeaveStatus.APPROVED);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUserAndDateRange(any(), any(), any()))
            .thenReturn(Arrays.asList(existingLeave));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));

        verify(userRepository).findById(1L);
        verify(leaveRepository).findByUserAndDateRange(any(), any(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void createLeaveRequest_ExceedsMaxDuration_ThrowsException() {
        // Arrange
        leaveRequest.setEndDate(LocalDate.now().plusDays(40)); // 33 days duration
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUserAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));

        verify(userRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void getUserLeaveHistory_ValidUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUser(employee)).thenReturn(Arrays.asList(leave));

        // Act
        List<LeaveResponse> history = leaveService.getUserLeaveHistory(1L);

        // Assert
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(leave.getId(), history.get(0).getId());

        verify(userRepository).findById(1L);
        verify(leaveRepository).findByUser(employee);
    }

    @Test
    void approveLeaveRequest_ValidRequest_Success() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved for vacation");
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRepository.findOverlappingApprovedLeaves(any(), any(), any())).thenReturn(Arrays.asList());
        when(leaveRepository.save(any(Leave.class))).thenReturn(leave);

        // Act
        LeaveResponse response = leaveService.approveLeaveRequest(1L, approvalRequest, 2L);

        // Assert
        assertNotNull(response);
        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(leaveRepository).save(any(Leave.class));
        verify(notificationService).sendLeaveApprovedNotification(any(Leave.class));
    }

    @Test
    void approveLeaveRequest_LeaveNotFound_ThrowsException() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved");
        when(leaveRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(LeaveNotFoundException.class, 
            () -> leaveService.approveLeaveRequest(1L, approvalRequest, 2L));

        verify(leaveRepository).findById(1L);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void approveLeaveRequest_ManagerNotFound_ThrowsException() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved");
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(LeaveApprovalException.class, 
            () -> leaveService.approveLeaveRequest(1L, approvalRequest, 2L));

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(2L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void approveLeaveRequest_EmployeeTriesToApproveOwnLeave_ThrowsException() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Self approval");
        employee.setRole(Role.MANAGER); // Make employee a manager
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(LeaveApprovalException.class, 
            () -> leaveService.approveLeaveRequest(1L, approvalRequest, 1L));

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(1L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void approveLeaveRequest_NonManagerUser_ThrowsException() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved");
        User regularEmployee = new User();
        regularEmployee.setId(3L);
        regularEmployee.setRole(Role.EMPLOYEE);
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(3L)).thenReturn(Optional.of(regularEmployee));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, 
            () -> leaveService.approveLeaveRequest(1L, approvalRequest, 3L));

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(3L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void approveLeaveRequest_LeaveNotPending_ThrowsException() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved");
        leave.setStatus(LeaveStatus.APPROVED);
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        // Act & Assert
        assertThrows(LeaveApprovalException.class, 
            () -> leaveService.approveLeaveRequest(1L, approvalRequest, 2L));

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(2L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void rejectLeaveRequest_ValidRequest_Success() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Insufficient staffing");
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRepository.save(any(Leave.class))).thenReturn(leave);

        // Act
        LeaveResponse response = leaveService.rejectLeaveRequest(1L, approvalRequest, 2L);

        // Assert
        assertNotNull(response);
        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(leaveRepository).save(any(Leave.class));
        verify(notificationService).sendLeaveRejectedNotification(any(Leave.class));
    }

    @Test
    void rejectLeaveRequest_NoRejectionReason_ThrowsException() {
        // Arrange
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("");
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        // Act & Assert
        assertThrows(LeaveApprovalException.class, 
            () -> leaveService.rejectLeaveRequest(1L, approvalRequest, 2L));

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(2L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void getLeaveById_UserCanAccessOwnLeave_Success() {
        // Arrange
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act
        LeaveResponse response = leaveService.getLeaveById(1L, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(leave.getId(), response.getId());

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void getLeaveById_ManagerCanAccessAnyLeave_Success() {
        // Arrange
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        // Act
        LeaveResponse response = leaveService.getLeaveById(1L, 2L);

        // Assert
        assertNotNull(response);
        assertEquals(leave.getId(), response.getId());

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    void getLeaveById_UserCannotAccessOthersLeave_ThrowsException() {
        // Arrange
        User otherEmployee = new User();
        otherEmployee.setId(3L);
        otherEmployee.setRole(Role.EMPLOYEE);
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherEmployee));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, 
            () -> leaveService.getLeaveById(1L, 3L));

        verify(leaveRepository).findById(1L);
        verify(userRepository).findById(3L);
    }

    @Test
    void getPendingLeaveRequests_Success() {
        // Arrange
        when(leaveRepository.findByStatusOrderByCreatedAtAsc(LeaveStatus.PENDING))
            .thenReturn(Arrays.asList(leave));

        // Act
        List<LeaveResponse> pendingLeaves = leaveService.getPendingLeaveRequests();

        // Assert
        assertNotNull(pendingLeaves);
        assertEquals(1, pendingLeaves.size());
        assertEquals(LeaveStatus.PENDING, pendingLeaves.get(0).getStatus());

        verify(leaveRepository).findByStatusOrderByCreatedAtAsc(LeaveStatus.PENDING);
    }

    @Test
    void getUserLeaveStatistics_Success() {
        // Arrange
        Leave approvedLeave = new Leave();
        approvedLeave.setStatus(LeaveStatus.APPROVED);
        approvedLeave.setStartDate(LocalDate.now());
        approvedLeave.setEndDate(LocalDate.now().plusDays(2));

        Leave rejectedLeave = new Leave();
        rejectedLeave.setStatus(LeaveStatus.REJECTED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUserAndYear(employee, 2024))
            .thenReturn(Arrays.asList(leave, approvedLeave, rejectedLeave));

        // Act
        LeaveService.LeaveStatistics stats = leaveService.getUserLeaveStatistics(1L, 2024);

        // Assert
        assertNotNull(stats);
        assertEquals(3, stats.getTotalRequests());
        assertEquals(1, stats.getApprovedRequests());
        assertEquals(1, stats.getPendingRequests());
        assertEquals(1, stats.getRejectedRequests());
        assertEquals(3, stats.getTotalApprovedDays());

        verify(userRepository).findById(1L);
        verify(leaveRepository).findByUserAndYear(employee, 2024);
    }

    // Additional tests for Requirements 3.1, 7.2, 7.5

    @Test
    void createLeaveRequest_ValidatesAllRequiredFields_Success() {
        // Arrange - Test requirement 3.1: store request with type, start date, end date, and reason
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUserAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());
        when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
            Leave savedLeave = invocation.getArgument(0);
            savedLeave.setId(1L);
            return savedLeave;
        });

        // Act
        LeaveResponse response = leaveService.createLeaveRequest(leaveRequest, 1L);

        // Assert - Verify all required fields are stored
        assertNotNull(response);
        assertEquals(leaveRequest.getLeaveType(), response.getLeaveType());
        assertEquals(leaveRequest.getStartDate(), response.getStartDate());
        assertEquals(leaveRequest.getEndDate(), response.getEndDate());
        assertEquals(leaveRequest.getReason(), response.getReason());
        assertEquals(LeaveStatus.PENDING, response.getStatus());

        verify(leaveRepository).save(argThat(savedLeave -> 
            savedLeave.getLeaveType().equals(leaveRequest.getLeaveType()) &&
            savedLeave.getStartDate().equals(leaveRequest.getStartDate()) &&
            savedLeave.getEndDate().equals(leaveRequest.getEndDate()) &&
            savedLeave.getReason().equals(leaveRequest.getReason()) &&
            savedLeave.getStatus().equals(LeaveStatus.PENDING)
        ));
    }

    @Test
    void createLeaveRequest_NullLeaveType_ThrowsException() {
        // Arrange
        leaveRequest.setLeaveType(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));
    }

    @Test
    void createLeaveRequest_NullStartDate_ThrowsException() {
        // Arrange
        leaveRequest.setStartDate(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));
    }

    @Test
    void createLeaveRequest_NullEndDate_ThrowsException() {
        // Arrange
        leaveRequest.setEndDate(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));
    }

    @Test
    void createLeaveRequest_EmptyReason_ThrowsException() {
        // Arrange
        leaveRequest.setReason("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThrows(InvalidLeaveRequestException.class, 
            () -> leaveService.createLeaveRequest(leaveRequest, 1L));
    }

    @Test
    void approveLeaveRequest_UpdatesStatusAndNotifiesEmployee_Success() {
        // Arrange - Test requirement 7.2: update status and notify employee
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved for vacation");
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRepository.findOverlappingApprovedLeaves(any(), any(), any())).thenReturn(Arrays.asList());
        when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
            Leave savedLeave = invocation.getArgument(0);
            return savedLeave;
        });

        // Act
        LeaveResponse response = leaveService.approveLeaveRequest(1L, approvalRequest, 2L);

        // Assert - Verify status is updated and notification is sent
        assertNotNull(response);
        verify(leaveRepository).save(argThat(savedLeave -> 
            savedLeave.getStatus().equals(LeaveStatus.APPROVED) &&
            savedLeave.getApprovedBy().equals(manager) &&
            savedLeave.getManagerComments().equals(approvalRequest.getComments())
        ));
        verify(notificationService).sendLeaveApprovedNotification(any(Leave.class));
    }

    @Test
    void rejectLeaveRequest_UpdatesStatusAndNotifiesEmployee_Success() {
        // Arrange - Test requirement 7.2: update status and notify employee
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Insufficient staffing");
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> {
            Leave savedLeave = invocation.getArgument(0);
            return savedLeave;
        });

        // Act
        LeaveResponse response = leaveService.rejectLeaveRequest(1L, approvalRequest, 2L);

        // Assert - Verify status is updated and notification is sent
        assertNotNull(response);
        verify(leaveRepository).save(argThat(savedLeave -> 
            savedLeave.getStatus().equals(LeaveStatus.REJECTED) &&
            savedLeave.getApprovedBy().equals(manager) &&
            savedLeave.getManagerComments().equals(approvalRequest.getComments())
        ));
        verify(notificationService).sendLeaveRejectedNotification(any(Leave.class));
    }

    @Test
    void approveLeaveRequest_ManagerApprovingOwnLeave_ThrowsException() {
        // Arrange - Test requirement 7.5: prevent managers from approving their own leave requests
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Self approval attempt");
        
        // Create a leave request where the manager is also the employee
        Leave managerLeave = new Leave();
        managerLeave.setId(2L);
        managerLeave.setUser(manager);
        managerLeave.setLeaveType(LeaveType.ANNUAL);
        managerLeave.setStartDate(LocalDate.now().plusDays(7));
        managerLeave.setEndDate(LocalDate.now().plusDays(9));
        managerLeave.setReason("Manager vacation");
        managerLeave.setStatus(LeaveStatus.PENDING);
        
        when(leaveRepository.findById(2L)).thenReturn(Optional.of(managerLeave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        // Act & Assert
        LeaveApprovalException exception = assertThrows(LeaveApprovalException.class, 
            () -> leaveService.approveLeaveRequest(2L, approvalRequest, 2L));
        
        assertEquals("You cannot approve your own leave request", exception.getMessage());
        
        verify(leaveRepository).findById(2L);
        verify(userRepository).findById(2L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void rejectLeaveRequest_ManagerRejectingOwnLeave_ThrowsException() {
        // Arrange - Test requirement 7.5: prevent managers from rejecting their own leave requests
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Self rejection attempt");
        
        // Create a leave request where the manager is also the employee
        Leave managerLeave = new Leave();
        managerLeave.setId(2L);
        managerLeave.setUser(manager);
        managerLeave.setLeaveType(LeaveType.ANNUAL);
        managerLeave.setStartDate(LocalDate.now().plusDays(7));
        managerLeave.setEndDate(LocalDate.now().plusDays(9));
        managerLeave.setReason("Manager vacation");
        managerLeave.setStatus(LeaveStatus.PENDING);
        
        when(leaveRepository.findById(2L)).thenReturn(Optional.of(managerLeave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        // Act & Assert
        LeaveApprovalException exception = assertThrows(LeaveApprovalException.class, 
            () -> leaveService.rejectLeaveRequest(2L, approvalRequest, 2L));
        
        assertEquals("You cannot approve your own leave request", exception.getMessage());
        
        verify(leaveRepository).findById(2L);
        verify(userRepository).findById(2L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void validateManagerPermissions_AdminCanApproveLeaves_Success() {
        // Arrange
        User admin = new User();
        admin.setId(3L);
        admin.setName("Admin User");
        admin.setEmail("admin@company.com");
        admin.setRole(Role.ADMIN);
        
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Admin approval");
        
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
        when(leaveRepository.findOverlappingApprovedLeaves(any(), any(), any())).thenReturn(Arrays.asList());
        when(leaveRepository.save(any(Leave.class))).thenReturn(leave);

        // Act
        LeaveResponse response = leaveService.approveLeaveRequest(1L, approvalRequest, 3L);

        // Assert
        assertNotNull(response);
        verify(leaveRepository).save(any(Leave.class));
        verify(notificationService).sendLeaveApprovedNotification(any(Leave.class));
    }

    @Test
    void validateLeaveRequestBusinessRules_WeekendDates_Success() {
        // Arrange - Test that weekend dates are handled properly
        leaveRequest.setStartDate(LocalDate.now().plusDays(7)); // Ensure future date
        leaveRequest.setEndDate(LocalDate.now().plusDays(9));   // Ensure future date
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUserAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());
        when(leaveRepository.save(any(Leave.class))).thenReturn(leave);

        // Act
        LeaveResponse response = leaveService.createLeaveRequest(leaveRequest, 1L);

        // Assert
        assertNotNull(response);
        verify(leaveRepository).save(any(Leave.class));
    }

    @Test
    void validateLeaveRequestBusinessRules_SingleDayLeave_Success() {
        // Arrange - Test single day leave request
        LocalDate singleDay = LocalDate.now().plusDays(7);
        leaveRequest.setStartDate(singleDay);
        leaveRequest.setEndDate(singleDay);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByUserAndDateRange(any(), any(), any())).thenReturn(Arrays.asList());
        when(leaveRepository.save(any(Leave.class))).thenReturn(leave);

        // Act
        LeaveResponse response = leaveService.createLeaveRequest(leaveRequest, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getDurationInDays());
        verify(leaveRepository).save(any(Leave.class));
    }
}