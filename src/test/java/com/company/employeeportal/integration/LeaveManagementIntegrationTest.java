package com.company.employeeportal.integration;

import com.company.employeeportal.dto.LeaveApprovalRequest;
import com.company.employeeportal.dto.LeaveRequest;
import com.company.employeeportal.dto.LeaveResponse;
import com.company.employeeportal.exception.InvalidLeaveRequestException;
import com.company.employeeportal.exception.LeaveApprovalException;
import com.company.employeeportal.exception.UnauthorizedAccessException;
import com.company.employeeportal.model.*;
import com.company.employeeportal.repository.LeaveRepository;
import com.company.employeeportal.repository.UserRepository;
import com.company.employeeportal.service.LeaveService;
import com.company.employeeportal.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for leave management functionality.
 * Tests the complete workflow from leave request creation to approval/rejection.
 * 
 * Covers Requirements:
 * - 3.1: Store leave request with type, start date, end date, and reason
 * - 7.2: Update status and notify employee on approval/rejection
 * - 7.5: Prevent managers from approving their own leave requests
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaveManagementIntegrationTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @MockBean
    private NotificationService notificationService;

    private User employee;
    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        leaveRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        employee = new User();
        employee.setName("John Employee");
        employee.setEmail("john.employee@company.com");
        employee.setPassword("password");
        employee.setRole(Role.EMPLOYEE);
        employee.setDepartment("IT");
        employee.setPosition("Developer");
        employee.setActive(true);
        employee = userRepository.save(employee);

        manager = new User();
        manager.setName("Jane Manager");
        manager.setEmail("jane.manager@company.com");
        manager.setPassword("password");
        manager.setRole(Role.MANAGER);
        manager.setDepartment("IT");
        manager.setPosition("Team Lead");
        manager.setActive(true);
        manager = userRepository.save(manager);

        admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@company.com");
        admin.setPassword("password");
        admin.setRole(Role.ADMIN);
        admin.setDepartment("HR");
        admin.setPosition("Administrator");
        admin.setActive(true);
        admin = userRepository.save(admin);
    }

    @Test
    void completeLeaveWorkflow_EmployeeRequestManagerApproval_Success() {
        // Test Requirement 3.1: Store leave request with all required fields
        
        // Step 1: Employee creates leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(7));
        leaveRequest.setEndDate(LocalDate.now().plusDays(9));
        leaveRequest.setReason("Family vacation");

        LeaveResponse createdLeave = leaveService.createLeaveRequest(leaveRequest, employee.getId());

        // Verify leave request is stored with all required fields (Requirement 3.1)
        assertNotNull(createdLeave);
        assertEquals(LeaveType.ANNUAL, createdLeave.getLeaveType());
        assertEquals(leaveRequest.getStartDate(), createdLeave.getStartDate());
        assertEquals(leaveRequest.getEndDate(), createdLeave.getEndDate());
        assertEquals(leaveRequest.getReason(), createdLeave.getReason());
        assertEquals(LeaveStatus.PENDING, createdLeave.getStatus());
        assertEquals(3L, createdLeave.getDurationInDays());

        // Verify notifications are sent
        verify(notificationService).sendLeaveRequestSubmittedNotification(any(Leave.class));
        verify(notificationService).sendNewLeaveRequestNotificationToManagers(any(Leave.class));

        // Step 2: Manager approves the leave request
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved for family time");
        
        LeaveResponse approvedLeave = leaveService.approveLeaveRequest(
            createdLeave.getId(), approvalRequest, manager.getId());

        // Test Requirement 7.2: Update status and notify employee
        assertNotNull(approvedLeave);
        assertEquals(LeaveStatus.APPROVED, approvedLeave.getStatus());
        assertEquals("Approved for family time", approvedLeave.getManagerComments());
        assertEquals(manager.getName(), approvedLeave.getApprovedByName());

        // Verify approval notification is sent (Requirement 7.2)
        verify(notificationService).sendLeaveApprovedNotification(any(Leave.class));

        // Step 3: Verify leave is persisted correctly
        Leave persistedLeave = leaveRepository.findById(createdLeave.getId()).orElse(null);
        assertNotNull(persistedLeave);
        assertEquals(LeaveStatus.APPROVED, persistedLeave.getStatus());
        assertEquals(manager.getId(), persistedLeave.getApprovedBy().getId());
    }

    @Test
    void completeLeaveWorkflow_EmployeeRequestManagerRejection_Success() {
        // Step 1: Employee creates leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.PERSONAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(5));
        leaveRequest.setEndDate(LocalDate.now().plusDays(7));
        leaveRequest.setReason("Personal matters");

        LeaveResponse createdLeave = leaveService.createLeaveRequest(leaveRequest, employee.getId());

        // Step 2: Manager rejects the leave request
        LeaveApprovalRequest rejectionRequest = new LeaveApprovalRequest("Insufficient staffing during this period");
        
        LeaveResponse rejectedLeave = leaveService.rejectLeaveRequest(
            createdLeave.getId(), rejectionRequest, manager.getId());

        // Test Requirement 7.2: Update status and notify employee
        assertNotNull(rejectedLeave);
        assertEquals(LeaveStatus.REJECTED, rejectedLeave.getStatus());
        assertEquals("Insufficient staffing during this period", rejectedLeave.getManagerComments());
        assertEquals(manager.getName(), rejectedLeave.getApprovedByName());

        // Verify rejection notification is sent (Requirement 7.2)
        verify(notificationService).sendLeaveRejectedNotification(any(Leave.class));
    }

    @Test
    void managerCannotApproveOwnLeave_ThrowsException() {
        // Test Requirement 7.5: Prevent managers from approving their own leave requests
        
        // Step 1: Manager creates their own leave request
        LeaveRequest managerLeaveRequest = new LeaveRequest();
        managerLeaveRequest.setLeaveType(LeaveType.ANNUAL);
        managerLeaveRequest.setStartDate(LocalDate.now().plusDays(10));
        managerLeaveRequest.setEndDate(LocalDate.now().plusDays(12));
        managerLeaveRequest.setReason("Manager vacation");

        LeaveResponse managerLeave = leaveService.createLeaveRequest(managerLeaveRequest, manager.getId());

        // Step 2: Manager attempts to approve their own leave request
        LeaveApprovalRequest selfApprovalRequest = new LeaveApprovalRequest("Self approval");
        
        // Should throw exception (Requirement 7.5)
        LeaveApprovalException exception = assertThrows(LeaveApprovalException.class, 
            () -> leaveService.approveLeaveRequest(managerLeave.getId(), selfApprovalRequest, manager.getId()));
        
        assertEquals("You cannot approve your own leave request", exception.getMessage());

        // Verify leave status remains pending
        Leave persistedLeave = leaveRepository.findById(managerLeave.getId()).orElse(null);
        assertNotNull(persistedLeave);
        assertEquals(LeaveStatus.PENDING, persistedLeave.getStatus());
        assertNull(persistedLeave.getApprovedBy());
    }

    @Test
    void adminCanApproveAnyLeave_Success() {
        // Step 1: Employee creates leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.SICK);
        leaveRequest.setStartDate(LocalDate.now().plusDays(3));
        leaveRequest.setEndDate(LocalDate.now().plusDays(5));
        leaveRequest.setReason("Medical appointment");

        LeaveResponse createdLeave = leaveService.createLeaveRequest(leaveRequest, employee.getId());

        // Step 2: Admin approves the leave request
        LeaveApprovalRequest adminApprovalRequest = new LeaveApprovalRequest("Admin approval for medical leave");
        
        LeaveResponse approvedLeave = leaveService.approveLeaveRequest(
            createdLeave.getId(), adminApprovalRequest, admin.getId());

        // Verify admin can approve leaves
        assertNotNull(approvedLeave);
        assertEquals(LeaveStatus.APPROVED, approvedLeave.getStatus());
        assertEquals(admin.getName(), approvedLeave.getApprovedByName());
    }

    @Test
    void leaveRequestValidation_AllRequiredFields_Success() {
        // Test Requirement 3.1: Validate all required fields are present
        
        LeaveRequest validRequest = new LeaveRequest();
        validRequest.setLeaveType(LeaveType.MATERNITY);
        validRequest.setStartDate(LocalDate.now().plusDays(30));
        validRequest.setEndDate(LocalDate.now().plusDays(60));
        validRequest.setReason("Maternity leave for new baby");

        LeaveResponse response = leaveService.createLeaveRequest(validRequest, employee.getId());

        assertNotNull(response);
        assertEquals(validRequest.getLeaveType(), response.getLeaveType());
        assertEquals(validRequest.getStartDate(), response.getStartDate());
        assertEquals(validRequest.getEndDate(), response.getEndDate());
        assertEquals(validRequest.getReason(), response.getReason());
    }

    @Test
    void leaveRequestValidation_MissingFields_ThrowsException() {
        // Test validation for missing required fields (Requirement 3.1)
        
        // Missing leave type
        LeaveRequest requestWithoutType = new LeaveRequest();
        requestWithoutType.setStartDate(LocalDate.now().plusDays(7));
        requestWithoutType.setEndDate(LocalDate.now().plusDays(9));
        requestWithoutType.setReason("Vacation");

        assertThrows(Exception.class, 
            () -> leaveService.createLeaveRequest(requestWithoutType, employee.getId()));

        // Missing start date
        LeaveRequest requestWithoutStartDate = new LeaveRequest();
        requestWithoutStartDate.setLeaveType(LeaveType.ANNUAL);
        requestWithoutStartDate.setEndDate(LocalDate.now().plusDays(9));
        requestWithoutStartDate.setReason("Vacation");

        assertThrows(Exception.class, 
            () -> leaveService.createLeaveRequest(requestWithoutStartDate, employee.getId()));

        // Missing end date
        LeaveRequest requestWithoutEndDate = new LeaveRequest();
        requestWithoutEndDate.setLeaveType(LeaveType.ANNUAL);
        requestWithoutEndDate.setStartDate(LocalDate.now().plusDays(7));
        requestWithoutEndDate.setReason("Vacation");

        assertThrows(Exception.class, 
            () -> leaveService.createLeaveRequest(requestWithoutEndDate, employee.getId()));

        // Missing reason
        LeaveRequest requestWithoutReason = new LeaveRequest();
        requestWithoutReason.setLeaveType(LeaveType.ANNUAL);
        requestWithoutReason.setStartDate(LocalDate.now().plusDays(7));
        requestWithoutReason.setEndDate(LocalDate.now().plusDays(9));

        assertThrows(Exception.class, 
            () -> leaveService.createLeaveRequest(requestWithoutReason, employee.getId()));
    }

    @Test
    void employeeCannotApproveLeaves_ThrowsException() {
        // Step 1: Create a leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(7));
        leaveRequest.setEndDate(LocalDate.now().plusDays(9));
        leaveRequest.setReason("Vacation");

        LeaveResponse createdLeave = leaveService.createLeaveRequest(leaveRequest, employee.getId());

        // Step 2: Another employee attempts to approve the leave
        User anotherEmployee = new User();
        anotherEmployee.setName("Another Employee");
        anotherEmployee.setEmail("another@company.com");
        anotherEmployee.setPassword("password");
        anotherEmployee.setRole(Role.EMPLOYEE);
        anotherEmployee.setDepartment("IT");
        anotherEmployee.setActive(true);
        anotherEmployee = userRepository.save(anotherEmployee);

        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Unauthorized approval");
        
        // Should throw exception
        assertThrows(UnauthorizedAccessException.class, 
            () -> leaveService.approveLeaveRequest(createdLeave.getId(), approvalRequest, anotherEmployee.getId()));
    }

    @Test
    void leaveApprovalWorkflow_BusinessRulesValidation_Success() {
        // Test various business rules for leave approval
        
        // Step 1: Create leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(7));
        leaveRequest.setEndDate(LocalDate.now().plusDays(9));
        leaveRequest.setReason("Family vacation");

        LeaveResponse createdLeave = leaveService.createLeaveRequest(leaveRequest, employee.getId());

        // Step 2: Verify leave is in pending status
        assertEquals(LeaveStatus.PENDING, createdLeave.getStatus());

        // Step 3: Manager approves leave
        LeaveApprovalRequest approvalRequest = new LeaveApprovalRequest("Approved");
        LeaveResponse approvedLeave = leaveService.approveLeaveRequest(
            createdLeave.getId(), approvalRequest, manager.getId());

        // Step 4: Verify leave cannot be approved again
        assertThrows(LeaveApprovalException.class, 
            () -> leaveService.approveLeaveRequest(approvedLeave.getId(), approvalRequest, manager.getId()));
    }

    @Test
    void rejectionRequiresReason_ThrowsExceptionWhenEmpty() {
        // Step 1: Create leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveType(LeaveType.PERSONAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(5));
        leaveRequest.setEndDate(LocalDate.now().plusDays(7));
        leaveRequest.setReason("Personal matters");

        LeaveResponse createdLeave = leaveService.createLeaveRequest(leaveRequest, employee.getId());

        // Step 2: Attempt to reject without reason
        LeaveApprovalRequest emptyRejectionRequest = new LeaveApprovalRequest("");
        
        assertThrows(LeaveApprovalException.class, 
            () -> leaveService.rejectLeaveRequest(createdLeave.getId(), emptyRejectionRequest, manager.getId()));

        // Step 3: Reject with proper reason
        LeaveApprovalRequest validRejectionRequest = new LeaveApprovalRequest("Insufficient coverage");
        LeaveResponse rejectedLeave = leaveService.rejectLeaveRequest(
            createdLeave.getId(), validRejectionRequest, manager.getId());

        assertEquals(LeaveStatus.REJECTED, rejectedLeave.getStatus());
        assertEquals("Insufficient coverage", rejectedLeave.getManagerComments());
    }

    @Test
    void getUserLeaveHistory_ReturnsCorrectData() {
        // Create multiple leave requests for the employee
        LeaveRequest request1 = new LeaveRequest(LeaveType.ANNUAL, 
            LocalDate.now().plusDays(7), LocalDate.now().plusDays(9), "Vacation 1");
        LeaveRequest request2 = new LeaveRequest(LeaveType.SICK, 
            LocalDate.now().plusDays(14), LocalDate.now().plusDays(16), "Medical appointment");

        LeaveResponse leave1 = leaveService.createLeaveRequest(request1, employee.getId());
        LeaveResponse leave2 = leaveService.createLeaveRequest(request2, employee.getId());

        // Get leave history
        List<LeaveResponse> history = leaveService.getUserLeaveHistory(employee.getId());

        assertEquals(2, history.size());
        assertTrue(history.stream().anyMatch(leave -> leave.getId().equals(leave1.getId())));
        assertTrue(history.stream().anyMatch(leave -> leave.getId().equals(leave2.getId())));
    }
}