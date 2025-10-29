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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for leave management operations.
 * Handles leave request creation, approval workflow, and business logic validation.
 */
@Service
@Transactional
public class LeaveService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public LeaveService(LeaveRepository leaveRepository, UserRepository userRepository, 
                       NotificationService notificationService) {
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Create a new leave request.
     */
    public LeaveResponse createLeaveRequest(LeaveRequest leaveRequest, Long userId) {
        logger.info("Creating leave request for user ID: {}", userId);

        // Get the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidLeaveRequestException("User not found"));

        // Validate the leave request
        validateLeaveRequest(leaveRequest, user);

        // Create the leave entity
        Leave leave = new Leave(user, leaveRequest.getLeaveType(), 
                               leaveRequest.getStartDate(), leaveRequest.getEndDate(), 
                               leaveRequest.getReason());

        // Save the leave request
        Leave savedLeave = leaveRepository.save(leave);
        
        logger.info("Leave request created with ID: {} for user: {}", savedLeave.getId(), user.getName());
        
        // Send notifications
        notificationService.sendLeaveRequestSubmittedNotification(savedLeave);
        notificationService.sendNewLeaveRequestNotificationToManagers(savedLeave);
        
        return convertToResponse(savedLeave);
    }

    /**
     * Get leave history for a specific user.
     */
    @Transactional(readOnly = true)
    public List<LeaveResponse> getUserLeaveHistory(Long userId) {
        logger.debug("Retrieving leave history for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidLeaveRequestException("User not found"));

        List<Leave> leaves = leaveRepository.findByUser(user);
        return leaves.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get leave history for a user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<LeaveResponse> getUserLeaveHistory(Long userId, Pageable pageable) {
        logger.debug("Retrieving paginated leave history for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidLeaveRequestException("User not found"));

        Page<Leave> leaves = leaveRepository.findByUser(user, pageable);
        return leaves.map(this::convertToResponse);
    }

    /**
     * Get pending leave requests for manager approval.
     */
    @Transactional(readOnly = true)
    public List<LeaveResponse> getPendingLeaveRequests() {
        logger.debug("Retrieving pending leave requests");

        List<Leave> pendingLeaves = leaveRepository.findByStatusOrderByCreatedAtAsc(LeaveStatus.PENDING);
        return pendingLeaves.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get pending leave requests with pagination.
     */
    @Transactional(readOnly = true)
    public Page<LeaveResponse> getPendingLeaveRequests(Pageable pageable) {
        logger.debug("Retrieving paginated pending leave requests");

        Page<Leave> pendingLeaves = leaveRepository.findByStatus(LeaveStatus.PENDING, pageable);
        return pendingLeaves.map(this::convertToResponse);
    }

    /**
     * Approve a leave request.
     */
    public LeaveResponse approveLeaveRequest(Long leaveId, LeaveApprovalRequest approvalRequest, Long managerId) {
        logger.info("Approving leave request ID: {} by manager ID: {}", leaveId, managerId);

        // Get the leave request
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new LeaveNotFoundException(leaveId));

        // Get the manager
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new LeaveApprovalException("Manager not found"));

        // Validate manager permissions
        validateManagerPermissions(manager, leave);

        // Validate leave can be approved
        if (!leave.isPending()) {
            throw new LeaveApprovalException("Leave request is not in pending status");
        }

        // Check for overlapping approved leaves
        validateNoOverlappingLeaves(leave);

        // Update leave status
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(manager);
        leave.setManagerComments(approvalRequest.getComments());

        Leave savedLeave = leaveRepository.save(leave);
        
        logger.info("Leave request ID: {} approved by manager: {}", leaveId, manager.getName());
        
        // Send approval notification
        notificationService.sendLeaveApprovedNotification(savedLeave);
        
        return convertToResponse(savedLeave);
    }

    /**
     * Reject a leave request.
     */
    public LeaveResponse rejectLeaveRequest(Long leaveId, LeaveApprovalRequest approvalRequest, Long managerId) {
        logger.info("Rejecting leave request ID: {} by manager ID: {}", leaveId, managerId);

        // Get the leave request
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new LeaveNotFoundException(leaveId));

        // Get the manager
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new LeaveApprovalException("Manager not found"));

        // Validate manager permissions
        validateManagerPermissions(manager, leave);

        // Validate leave can be rejected
        if (!leave.isPending()) {
            throw new LeaveApprovalException("Leave request is not in pending status");
        }

        // Rejection reason is required
        if (approvalRequest.getComments() == null || approvalRequest.getComments().trim().isEmpty()) {
            throw new LeaveApprovalException("Rejection reason is required");
        }

        // Update leave status
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(manager);
        leave.setManagerComments(approvalRequest.getComments());

        Leave savedLeave = leaveRepository.save(leave);
        
        logger.info("Leave request ID: {} rejected by manager: {}", leaveId, manager.getName());
        
        // Send rejection notification
        notificationService.sendLeaveRejectedNotification(savedLeave);
        
        return convertToResponse(savedLeave);
    }

    /**
     * Get a specific leave request by ID.
     */
    @Transactional(readOnly = true)
    public LeaveResponse getLeaveById(Long leaveId, Long userId) {
        logger.debug("Retrieving leave request ID: {} for user ID: {}", leaveId, userId);

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new LeaveNotFoundException(leaveId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidLeaveRequestException("User not found"));

        // Check if user can access this leave request
        if (!canUserAccessLeave(user, leave)) {
            throw new UnauthorizedAccessException("You don't have permission to access this leave request");
        }

        return convertToResponse(leave);
    }

    /**
     * Get approved leaves within a date range (for calendar view).
     */
    @Transactional(readOnly = true)
    public List<LeaveResponse> getApprovedLeavesInDateRange(LocalDate startDate, LocalDate endDate) {
        logger.debug("Retrieving approved leaves between {} and {}", startDate, endDate);

        List<Leave> approvedLeaves = leaveRepository.findApprovedLeavesInDateRange(startDate, endDate);
        return approvedLeaves.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get leave statistics for a user.
     */
    @Transactional(readOnly = true)
    public LeaveStatistics getUserLeaveStatistics(Long userId, int year) {
        logger.debug("Retrieving leave statistics for user ID: {} for year: {}", userId, year);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidLeaveRequestException("User not found"));

        List<Leave> yearlyLeaves = leaveRepository.findByUserAndYear(user, year);
        
        long totalRequests = yearlyLeaves.size();
        long approvedRequests = yearlyLeaves.stream()
                .filter(Leave::isApproved)
                .count();
        long pendingRequests = yearlyLeaves.stream()
                .filter(Leave::isPending)
                .count();
        long rejectedRequests = yearlyLeaves.stream()
                .filter(Leave::isRejected)
                .count();
        long totalApprovedDays = yearlyLeaves.stream()
                .filter(Leave::isApproved)
                .mapToLong(Leave::getDurationInDays)
                .sum();

        return new LeaveStatistics(totalRequests, approvedRequests, pendingRequests, 
                                 rejectedRequests, totalApprovedDays);
    }

    /**
     * Get all leave requests for HR management.
     */
    @Transactional(readOnly = true)
    public List<LeaveResponse> getAllLeaveRequests() {
        logger.debug("Retrieving all leave requests for HR management");

        List<Leave> allLeaves = leaveRepository.findAll();
        return allLeaves.stream()
                .map(this::convertToResponseWithUserInfo)
                .collect(Collectors.toList());
    }

    /**
     * Get all leave requests with pagination for HR management.
     */
    @Transactional(readOnly = true)
    public Page<LeaveResponse> getAllLeaveRequests(Pageable pageable) {
        logger.debug("Retrieving paginated all leave requests for HR management");

        Page<Leave> allLeaves = leaveRepository.findAll(pageable);
        return allLeaves.map(this::convertToResponseWithUserInfo);
    }

    // Private helper methods

    /**
     * Validate leave request business rules.
     */
    private void validateLeaveRequest(LeaveRequest leaveRequest, User user) {
        // Check if end date is after start date
        if (leaveRequest.getEndDate().isBefore(leaveRequest.getStartDate())) {
            throw new InvalidLeaveRequestException("End date must be after or equal to start date");
        }

        // Check if start date is not in the past (allow same day requests)
        if (leaveRequest.getStartDate().isBefore(LocalDate.now())) {
            throw new InvalidLeaveRequestException("Leave requests cannot be submitted for past dates");
        }

        // Check for overlapping pending or approved leaves
        List<Leave> overlappingLeaves = leaveRepository.findByUserAndDateRange(
                user, leaveRequest.getStartDate(), leaveRequest.getEndDate());
        
        boolean hasOverlap = overlappingLeaves.stream()
                .anyMatch(leave -> leave.getStatus() == LeaveStatus.PENDING || 
                                 leave.getStatus() == LeaveStatus.APPROVED);
        
        if (hasOverlap) {
            throw new InvalidLeaveRequestException("You already have a pending or approved leave request for the selected dates");
        }

        // Validate maximum leave duration (e.g., 30 days)
        long duration = leaveRequest.getStartDate().datesUntil(leaveRequest.getEndDate().plusDays(1)).count();
        if (duration > 30) {
            throw new InvalidLeaveRequestException("Leave duration cannot exceed 30 days");
        }
    }

    /**
     * Validate HR permissions for leave approval.
     */
    private void validateManagerPermissions(User hrUser, Leave leave) {
        // Check if user has HR role
        if (hrUser.getRole() != Role.HR) {
            throw new UnauthorizedAccessException("Only HR users can approve leave requests");
        }

        // Prevent self-approval
        if (hrUser.getId().equals(leave.getUser().getId())) {
            throw new LeaveApprovalException("HR users cannot approve their own leave requests");
        }
    }

    /**
     * Validate no overlapping approved leaves exist.
     */
    private void validateNoOverlappingLeaves(Leave leave) {
        List<Leave> overlappingLeaves = leaveRepository.findOverlappingApprovedLeaves(
                leave.getUser(), leave.getStartDate(), leave.getEndDate());
        
        if (!overlappingLeaves.isEmpty()) {
            throw new LeaveApprovalException("User already has approved leave for overlapping dates");
        }
    }

    /**
     * Check if user can access a specific leave request.
     */
    private boolean canUserAccessLeave(User user, Leave leave) {
        // User can access their own leaves
        if (user.getId().equals(leave.getUser().getId())) {
            return true;
        }

        // HR can access all leaves
        return user.getRole() == Role.HR;
    }

    /**
     * Convert Leave entity to LeaveResponse DTO.
     */
    private LeaveResponse convertToResponse(Leave leave) {
        LeaveResponse response = new LeaveResponse();
        response.setId(leave.getId());
        response.setLeaveType(leave.getLeaveType());
        response.setStartDate(leave.getStartDate());
        response.setEndDate(leave.getEndDate());
        response.setReason(leave.getReason());
        response.setStatus(leave.getStatus());
        response.setManagerComments(leave.getManagerComments());
        response.setApprovedByName(leave.getApprovedBy() != null ? leave.getApprovedBy().getName() : null);
        response.setCreatedAt(leave.getCreatedAt());
        response.setUpdatedAt(leave.getUpdatedAt());
        response.setDurationInDays(leave.getDurationInDays());
        return response;
    }

    /**
     * Convert Leave entity to LeaveResponse DTO with user information for HR management.
     */
    private LeaveResponse convertToResponseWithUserInfo(Leave leave) {
        LeaveResponse response = convertToResponse(leave);
        response.setUserName(leave.getUser().getName());
        response.setUserEmail(leave.getUser().getEmail());
        response.setUserDepartment(leave.getUser().getDepartment());
        return response;
    }

    /**
     * Inner class for leave statistics.
     */
    public static class LeaveStatistics {
        private final long totalRequests;
        private final long approvedRequests;
        private final long pendingRequests;
        private final long rejectedRequests;
        private final long totalApprovedDays;

        public LeaveStatistics(long totalRequests, long approvedRequests, long pendingRequests, 
                             long rejectedRequests, long totalApprovedDays) {
            this.totalRequests = totalRequests;
            this.approvedRequests = approvedRequests;
            this.pendingRequests = pendingRequests;
            this.rejectedRequests = rejectedRequests;
            this.totalApprovedDays = totalApprovedDays;
        }

        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getApprovedRequests() { return approvedRequests; }
        public long getPendingRequests() { return pendingRequests; }
        public long getRejectedRequests() { return rejectedRequests; }
        public long getTotalApprovedDays() { return totalApprovedDays; }
    }
}
