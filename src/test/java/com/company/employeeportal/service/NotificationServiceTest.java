package com.company.employeeportal.service;

import com.company.employeeportal.model.*;
import com.company.employeeportal.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private NotificationService notificationService;

    private User employee;
    private User manager;
    private Leave leave;

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

        // Create test leave
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

        // Set up test configuration values
        ReflectionTestUtils.setField(notificationService, "fromEmail", "noreply@company.com");
        ReflectionTestUtils.setField(notificationService, "companyName", "Test Company");
        ReflectionTestUtils.setField(notificationService, "portalUrl", "http://localhost:8080");

        // Mock mail sender behavior
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // Mock template engine behavior
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Email Content</html>");
    }

    @Test
    void sendLeaveRequestSubmittedNotification_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendLeaveRequestSubmittedNotification(leave);
        });

        // Verify template processing and email sending
        verify(templateEngine).process(eq("email/leave-submitted"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendLeaveApprovedNotification_Success() {
        // Arrange
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(manager);
        leave.setManagerComments("Approved for vacation");

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendLeaveApprovedNotification(leave);
        });

        // Verify template processing and email sending
        verify(templateEngine).process(eq("email/leave-approved"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendLeaveRejectedNotification_Success() {
        // Arrange
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(manager);
        leave.setManagerComments("Insufficient staffing during that period");

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendLeaveRejectedNotification(leave);
        });

        // Verify template processing and email sending
        verify(templateEngine).process(eq("email/leave-rejected"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendNewLeaveRequestNotificationToManagers_Success() {
        // Arrange
        List<User> managers = Arrays.asList(manager);
        when(userRepository.findByRoleInAndActiveTrue(anyList())).thenReturn(managers);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendNewLeaveRequestNotificationToManagers(leave);
        });

        // Verify repository call and email sending
        verify(userRepository).findByRoleInAndActiveTrue(List.of(Role.MANAGER, Role.ADMIN));
        verify(templateEngine).process(eq("email/leave-manager-notification"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendLeaveApprovedNotification_WithoutManagerComments_Success() {
        // Arrange
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(manager);
        leave.setManagerComments(null);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendLeaveApprovedNotification(leave);
        });

        // Verify template processing
        verify(templateEngine).process(eq("email/leave-approved"), any(Context.class));
    }

    @Test
    void sendLeaveRejectedNotification_WithNullComments_Success() {
        // Arrange
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(manager);
        leave.setManagerComments(null);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendLeaveRejectedNotification(leave);
        });

        // Verify template processing
        verify(templateEngine).process(eq("email/leave-rejected"), any(Context.class));
    }

    @Test
    void sendEmail_TemplateProcessingFails_UsesFallback() {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendLeaveRequestSubmittedNotification(leave);
        });

        // Verify fallback is used and email is still sent
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendNewLeaveRequestNotificationToManagers_NoManagers_DoesNotSendEmail() {
        // Arrange
        when(userRepository.findByRoleInAndActiveTrue(anyList())).thenReturn(Arrays.asList());

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.sendNewLeaveRequestNotificationToManagers(leave);
        });

        // Verify no emails are sent when no managers found
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}