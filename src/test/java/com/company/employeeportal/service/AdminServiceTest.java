package com.company.employeeportal.service;

import com.company.employeeportal.dto.DashboardMetrics;
import com.company.employeeportal.model.*;
import com.company.employeeportal.repository.AnnouncementRepository;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminService.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AdminService adminService;

    private User testEmployee;
    private User testManager;
    private User testAdmin;
    private Leave testLeave;
    private Announcement testAnnouncement;

    @BeforeEach
    void setUp() {
        // Create test users
        testEmployee = new User("John Doe", "john@array.world", "password", Role.EMPLOYEE);
        testEmployee.setId(1L);
        testEmployee.setDepartment("Engineering");
        testEmployee.setActive(true);

        testManager = new User("Jane Smith", "jane@array.world", "password", Role.MANAGER);
        testManager.setId(2L);
        testManager.setDepartment("Engineering");
        testManager.setActive(true);

        testAdmin = new User("Admin User", "admin@array.world", "password", Role.ADMIN);
        testAdmin.setId(3L);
        testAdmin.setDepartment("IT");
        testAdmin.setActive(true);

        // Create test leave
        testLeave = new Leave(testEmployee, LeaveType.ANNUAL, 
                             LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), "Vacation");
        testLeave.setId(1L);
        testLeave.setStatus(LeaveStatus.PENDING);

        // Create test announcement
        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("Test Announcement");
        testAnnouncement.setContent("Test content");
        testAnnouncement.setCreatedBy(testAdmin);
        testAnnouncement.setActive(true);
        testAnnouncement.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getDashboardMetrics_ShouldReturnCompleteMetrics() {
        // Arrange
        List<User> allUsers = Arrays.asList(testEmployee, testManager, testAdmin);
        
        when(userRepository.count()).thenReturn(3L);
        when(userRepository.countByActiveTrue()).thenReturn(3L);
        when(userRepository.countByRoleAndActiveTrue(Role.EMPLOYEE)).thenReturn(1L);
        when(userRepository.countByRoleAndActiveTrue(Role.MANAGER)).thenReturn(1L);
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);
        when(userRepository.findByActiveTrue()).thenReturn(allUsers);

        when(leaveRepository.count()).thenReturn(5L);
        when(leaveRepository.countByStatus(LeaveStatus.PENDING)).thenReturn(2L);
        when(leaveRepository.countByStatus(LeaveStatus.APPROVED)).thenReturn(2L);
        when(leaveRepository.countByStatus(LeaveStatus.REJECTED)).thenReturn(1L);
        when(leaveRepository.countByLeaveType(any(LeaveType.class))).thenReturn(1L);

        when(announcementRepository.count()).thenReturn(3L);
        when(announcementRepository.countByActiveTrue()).thenReturn(2L);

        // Act
        DashboardMetrics metrics = adminService.getDashboardMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals(3L, metrics.getTotalUsers());
        assertEquals(3L, metrics.getActiveUsers());
        assertEquals(1L, metrics.getTotalEmployees());
        assertEquals(1L, metrics.getTotalManagers());
        assertEquals(1L, metrics.getTotalAdmins());
        
        assertEquals(5L, metrics.getTotalLeaveRequests());
        assertEquals(2L, metrics.getPendingLeaveRequests());
        assertEquals(2L, metrics.getApprovedLeaveRequests());
        assertEquals(1L, metrics.getRejectedLeaveRequests());
        
        assertEquals(3L, metrics.getTotalAnnouncements());
        assertEquals(2L, metrics.getActiveAnnouncements());
        
        assertNotNull(metrics.getUsersByDepartment());
        assertEquals(2L, metrics.getUsersByDepartment().get("Engineering"));
        assertEquals(1L, metrics.getUsersByDepartment().get("IT"));
        
        assertNotNull(metrics.getLeavesByType());
        assertNotNull(metrics.getLeavesByStatus());
        assertNotNull(metrics.getLastUpdated());
        assertEquals("HEALTHY", metrics.getSystemStatus());
    }

    @Test
    void getDashboardMetrics_WithNullDepartments_ShouldHandleUnassigned() {
        // Arrange
        testEmployee.setDepartment(null);
        List<User> users = Arrays.asList(testEmployee, testManager);
        
        when(userRepository.count()).thenReturn(2L);
        when(userRepository.countByActiveTrue()).thenReturn(2L);
        when(userRepository.countByRoleAndActiveTrue(any(Role.class))).thenReturn(1L);
        when(userRepository.findByActiveTrue()).thenReturn(users);
        
        when(leaveRepository.count()).thenReturn(0L);
        when(leaveRepository.countByStatus(any(LeaveStatus.class))).thenReturn(0L);
        when(leaveRepository.countByLeaveType(any(LeaveType.class))).thenReturn(0L);
        
        when(announcementRepository.count()).thenReturn(0L);
        when(announcementRepository.countByActiveTrue()).thenReturn(0L);

        // Act
        DashboardMetrics metrics = adminService.getDashboardMetrics();

        // Assert
        assertNotNull(metrics.getUsersByDepartment());
        assertEquals(1L, metrics.getUsersByDepartment().get("Unassigned"));
        assertEquals(1L, metrics.getUsersByDepartment().get("Engineering"));
    }

    @Test
    void getDashboardMetrics_WithDatabaseError_ShouldSetErrorStatus() {
        // Arrange
        when(userRepository.count()).thenThrow(new RuntimeException("Database error"));

        // Act
        DashboardMetrics metrics = adminService.getDashboardMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals("ERROR", metrics.getSystemStatus());
    }

    @Test
    void getUserStatistics_ShouldReturnDetailedStats() {
        // Arrange
        List<User> allUsers = Arrays.asList(testEmployee, testManager, testAdmin);
        
        when(userRepository.count()).thenReturn(3L);
        when(userRepository.countByActiveTrue()).thenReturn(3L);
        when(userRepository.countByRoleAndActiveTrue(Role.EMPLOYEE)).thenReturn(1L);
        when(userRepository.countByRoleAndActiveTrue(Role.MANAGER)).thenReturn(1L);
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);
        when(userRepository.findByActiveTrue()).thenReturn(allUsers);

        // Act
        Map<String, Object> stats = adminService.getUserStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals(3L, stats.get("totalUsers"));
        assertEquals(3L, stats.get("activeUsers"));
        assertEquals(0L, stats.get("inactiveUsers"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> roleBreakdown = (Map<String, Long>) stats.get("roleBreakdown");
        assertNotNull(roleBreakdown);
        assertEquals(1L, roleBreakdown.get("EMPLOYEE"));
        assertEquals(1L, roleBreakdown.get("MANAGER"));
        assertEquals(1L, roleBreakdown.get("ADMIN"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> departmentBreakdown = (Map<String, Long>) stats.get("departmentBreakdown");
        assertNotNull(departmentBreakdown);
        assertEquals(2L, departmentBreakdown.get("Engineering"));
        assertEquals(1L, departmentBreakdown.get("IT"));
    }

    @Test
    void getUserStatistics_WithError_ShouldReturnErrorMessage() {
        // Arrange
        when(userRepository.count()).thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, Object> stats = adminService.getUserStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals("Failed to calculate user statistics", stats.get("error"));
    }

    @Test
    void getSystemMonitoringData_ShouldReturnMonitoringInfo() {
        // Arrange
        when(userRepository.count()).thenReturn(3L);
        when(leaveRepository.count()).thenReturn(5L);
        when(announcementRepository.count()).thenReturn(2L);
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);
        when(leaveRepository.countByStatus(LeaveStatus.PENDING)).thenReturn(2L);
        when(announcementRepository.countByActiveTrue()).thenReturn(2L);

        // Act
        Map<String, Object> monitoringData = adminService.getSystemMonitoringData();

        // Assert
        assertNotNull(monitoringData);
        assertEquals("HEALTHY", monitoringData.get("databaseStatus"));
        assertEquals("HEALTHY", monitoringData.get("systemStatus"));
        assertEquals(2L, monitoringData.get("pendingLeaves"));
        assertEquals(2L, monitoringData.get("activeAnnouncements"));
        assertNotNull(monitoringData.get("lastChecked"));
    }

    @Test
    void getSystemMonitoringData_WithNoAdmins_ShouldReturnDegradedStatus() {
        // Arrange
        when(userRepository.count()).thenReturn(2L);
        when(leaveRepository.count()).thenReturn(5L);
        when(announcementRepository.count()).thenReturn(2L);
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(0L); // No admins
        when(leaveRepository.countByStatus(LeaveStatus.PENDING)).thenReturn(2L);
        when(announcementRepository.countByActiveTrue()).thenReturn(2L);

        // Act
        Map<String, Object> monitoringData = adminService.getSystemMonitoringData();

        // Assert
        assertNotNull(monitoringData);
        assertEquals("HEALTHY", monitoringData.get("databaseStatus"));
        assertEquals("DEGRADED", monitoringData.get("systemStatus"));
    }

    @Test
    void getSystemMonitoringData_WithDatabaseError_ShouldReturnUnhealthyStatus() {
        // Arrange
        when(userRepository.count()).thenThrow(new RuntimeException("Database connection failed"));

        // Act
        Map<String, Object> monitoringData = adminService.getSystemMonitoringData();

        // Assert
        assertNotNull(monitoringData);
        assertEquals("UNHEALTHY", monitoringData.get("databaseStatus"));
        assertEquals("DEGRADED", monitoringData.get("systemStatus"));
    }

    @Test
    void getSystemMonitoringData_WithCompleteError_ShouldReturnErrorStatus() {
        // Arrange
        when(userRepository.count()).thenThrow(new RuntimeException("Complete system failure"));

        // Act
        Map<String, Object> monitoringData = adminService.getSystemMonitoringData();

        // Assert
        assertNotNull(monitoringData);
        assertEquals("Failed to gather monitoring data", monitoringData.get("error"));
        assertEquals("ERROR", monitoringData.get("systemStatus"));
    }

    @Test
    void getDashboardMetrics_ShouldIncludeAllLeaveTypes() {
        // Arrange
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.countByActiveTrue()).thenReturn(1L);
        when(userRepository.countByRoleAndActiveTrue(any(Role.class))).thenReturn(0L);
        when(userRepository.findByActiveTrue()).thenReturn(Arrays.asList(testEmployee));
        
        when(leaveRepository.count()).thenReturn(6L);
        when(leaveRepository.countByStatus(any(LeaveStatus.class))).thenReturn(2L);
        
        // Mock counts for each leave type
        when(leaveRepository.countByLeaveType(LeaveType.ANNUAL)).thenReturn(2L);
        when(leaveRepository.countByLeaveType(LeaveType.SICK)).thenReturn(1L);
        when(leaveRepository.countByLeaveType(LeaveType.PERSONAL)).thenReturn(1L);
        when(leaveRepository.countByLeaveType(LeaveType.MATERNITY)).thenReturn(1L);
        when(leaveRepository.countByLeaveType(LeaveType.PATERNITY)).thenReturn(1L);
        when(leaveRepository.countByLeaveType(LeaveType.EMERGENCY)).thenReturn(0L);
        
        when(announcementRepository.count()).thenReturn(0L);
        when(announcementRepository.countByActiveTrue()).thenReturn(0L);

        // Act
        DashboardMetrics metrics = adminService.getDashboardMetrics();

        // Assert
        assertNotNull(metrics.getLeavesByType());
        assertEquals(6, metrics.getLeavesByType().size());
        assertEquals(2L, metrics.getLeavesByType().get("ANNUAL"));
        assertEquals(1L, metrics.getLeavesByType().get("SICK"));
        assertEquals(1L, metrics.getLeavesByType().get("PERSONAL"));
        assertEquals(1L, metrics.getLeavesByType().get("MATERNITY"));
        assertEquals(1L, metrics.getLeavesByType().get("PATERNITY"));
        assertEquals(0L, metrics.getLeavesByType().get("EMERGENCY"));
    }

    @Test
    void getDashboardMetrics_ShouldIncludeAllLeaveStatuses() {
        // Arrange
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.countByActiveTrue()).thenReturn(1L);
        when(userRepository.countByRoleAndActiveTrue(any(Role.class))).thenReturn(0L);
        when(userRepository.findByActiveTrue()).thenReturn(Arrays.asList(testEmployee));
        
        when(leaveRepository.count()).thenReturn(6L);
        when(leaveRepository.countByStatus(LeaveStatus.PENDING)).thenReturn(2L);
        when(leaveRepository.countByStatus(LeaveStatus.APPROVED)).thenReturn(3L);
        when(leaveRepository.countByStatus(LeaveStatus.REJECTED)).thenReturn(1L);
        when(leaveRepository.countByLeaveType(any(LeaveType.class))).thenReturn(1L);
        
        when(announcementRepository.count()).thenReturn(0L);
        when(announcementRepository.countByActiveTrue()).thenReturn(0L);

        // Act
        DashboardMetrics metrics = adminService.getDashboardMetrics();

        // Assert
        assertNotNull(metrics.getLeavesByStatus());
        assertEquals(3, metrics.getLeavesByStatus().size());
        assertEquals(2L, metrics.getLeavesByStatus().get("PENDING"));
        assertEquals(3L, metrics.getLeavesByStatus().get("APPROVED"));
        assertEquals(1L, metrics.getLeavesByStatus().get("REJECTED"));
    }
}
