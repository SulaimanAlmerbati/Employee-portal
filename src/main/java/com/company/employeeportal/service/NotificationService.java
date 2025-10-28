package com.company.employeeportal.service;

import com.company.employeeportal.model.Leave;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for handling notifications related to leave requests.
 * Provides email notification functionality for leave status changes with template support.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.notification.company-name:Employee Portal}")
    private String companyName;

    @Value("${app.notification.portal-url:http://localhost:8080}")
    private String portalUrl;

    @Autowired
    public NotificationService(JavaMailSender mailSender, TemplateEngine templateEngine, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.userRepository = userRepository;
    }

    /**
     * Send notification when a leave request is submitted.
     */
    @Async("notificationTaskExecutor")
    public void sendLeaveRequestSubmittedNotification(Leave leave) {
        logger.info("Sending leave request submitted notification for leave ID: {}", leave.getId());
        
        try {
            String subject = "Leave Request Submitted - " + leave.getLeaveType();
            String htmlContent = generateEmailFromTemplate("leave-submitted", createLeaveContext(leave));
            
            sendEmail(leave.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Leave request submitted notification sent successfully for leave ID: {}", leave.getId());
        } catch (Exception e) {
            logger.error("Failed to send leave request submitted notification for leave ID: {}", leave.getId(), e);
        }
    }

    /**
     * Send notification when a leave request is approved.
     */
    @Async("notificationTaskExecutor")
    public void sendLeaveApprovedNotification(Leave leave) {
        logger.info("Sending leave approved notification for leave ID: {}", leave.getId());
        
        try {
            String subject = "Leave Request Approved - " + leave.getLeaveType();
            String htmlContent = generateEmailFromTemplate("leave-approved", createLeaveContext(leave));
            
            sendEmail(leave.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Leave approved notification sent successfully for leave ID: {}", leave.getId());
        } catch (Exception e) {
            logger.error("Failed to send leave approved notification for leave ID: {}", leave.getId(), e);
        }
    }

    /**
     * Send notification when a leave request is rejected.
     */
    @Async("notificationTaskExecutor")
    public void sendLeaveRejectedNotification(Leave leave) {
        logger.info("Sending leave rejected notification for leave ID: {}", leave.getId());
        
        try {
            String subject = "Leave Request Rejected - " + leave.getLeaveType();
            String htmlContent = generateEmailFromTemplate("leave-rejected", createLeaveContext(leave));
            
            sendEmail(leave.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Leave rejected notification sent successfully for leave ID: {}", leave.getId());
        } catch (Exception e) {
            logger.error("Failed to send leave rejected notification for leave ID: {}", leave.getId(), e);
        }
    }

    /**
     * Send notification to managers when a new leave request is submitted.
     */
    @Async("notificationTaskExecutor")
    public void sendNewLeaveRequestNotificationToManagers(Leave leave) {
        logger.info("Sending new leave request notification to managers for leave ID: {}", leave.getId());
        
        try {
            String subject = "New Leave Request Pending Approval - " + leave.getUser().getName();
            String htmlContent = generateEmailFromTemplate("leave-manager-notification", createLeaveContext(leave));
            
            // Get all active HR personnel
            List<User> managers = userRepository.findByRoleInAndActiveTrue(List.of(Role.HR));
            
            for (User manager : managers) {
                sendEmail(manager.getEmail(), subject, htmlContent);
            }
            
            logger.info("New leave request notification sent to {} managers for leave ID: {}", managers.size(), leave.getId());
        } catch (Exception e) {
            logger.error("Failed to send new leave request notification to managers for leave ID: {}", leave.getId(), e);
        }
    }

    // Private helper methods for email processing

    /**
     * Send email using JavaMailSender with HTML content.
     */
    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true indicates HTML content
        
        mailSender.send(message);
        
        // Also log for debugging purposes
        logger.debug("Email sent to: {} with subject: {}", to, subject);
    }

    /**
     * Generate email content from Thymeleaf template.
     */
    private String generateEmailFromTemplate(String templateName, Context context) {
        try {
            return templateEngine.process("email/" + templateName, context);
        } catch (Exception e) {
            logger.error("Failed to process email template: {}", templateName, e);
            // Fallback to plain text if template processing fails
            return generateFallbackContent(templateName, context);
        }
    }

    /**
     * Create Thymeleaf context with leave data for email templates.
     */
    private Context createLeaveContext(Leave leave) {
        Context context = new Context();
        
        // User information
        context.setVariable("userName", leave.getUser().getName());
        context.setVariable("userEmail", leave.getUser().getEmail());
        context.setVariable("userDepartment", leave.getUser().getDepartment());
        
        // Leave information
        context.setVariable("leaveId", leave.getId());
        context.setVariable("leaveType", leave.getLeaveType().toString());
        context.setVariable("startDate", leave.getStartDate().format(DATE_FORMATTER));
        context.setVariable("endDate", leave.getEndDate().format(DATE_FORMATTER));
        context.setVariable("duration", leave.getDurationInDays());
        context.setVariable("reason", leave.getReason());
        context.setVariable("status", leave.getStatus().toString());
        
        // Manager information (if available)
        if (leave.getApprovedBy() != null) {
            context.setVariable("approvedBy", leave.getApprovedBy().getName());
            context.setVariable("managerComments", leave.getManagerComments());
        }
        
        // System information
        context.setVariable("companyName", companyName);
        context.setVariable("portalUrl", portalUrl);
        context.setVariable("currentYear", java.time.Year.now().getValue());
        
        return context;
    }

    /**
     * Generate fallback plain text content if template processing fails.
     */
    private String generateFallbackContent(String templateName, Context context) {
        String userName = (String) context.getVariable("userName");
        String leaveType = (String) context.getVariable("leaveType");
        String startDate = (String) context.getVariable("startDate");
        String endDate = (String) context.getVariable("endDate");
        Long duration = (Long) context.getVariable("duration");
        String reason = (String) context.getVariable("reason");
        
        switch (templateName) {
            case "leave-submitted":
                return String.format(
                    "Dear %s,\n\nYour %s leave request from %s to %s (%d days) has been submitted successfully.\n\nReason: %s\n\nBest regards,\n%s",
                    userName, leaveType, startDate, endDate, duration, reason, companyName
                );
            case "leave-approved":
                String approvedBy = (String) context.getVariable("approvedBy");
                return String.format(
                    "Dear %s,\n\nYour %s leave request from %s to %s has been approved by %s.\n\nBest regards,\n%s",
                    userName, leaveType, startDate, endDate, approvedBy, companyName
                );
            case "leave-rejected":
                String rejectedBy = (String) context.getVariable("approvedBy");
                String comments = (String) context.getVariable("managerComments");
                return String.format(
                    "Dear %s,\n\nYour %s leave request from %s to %s has been rejected by %s.\n\nReason: %s\n\nBest regards,\n%s",
                    userName, leaveType, startDate, endDate, rejectedBy, comments != null ? comments : "No reason provided", companyName
                );
            case "leave-manager-notification":
                String userDepartment = (String) context.getVariable("userDepartment");
                return String.format(
                    "Dear Manager,\n\nA new %s leave request has been submitted by %s (%s) from %s to %s.\n\nReason: %s\n\nPlease review in the portal.\n\nBest regards,\n%s",
                    leaveType, userName, userDepartment, startDate, endDate, reason, companyName
                );
            default:
                return "Notification from " + companyName;
        }
    }
}