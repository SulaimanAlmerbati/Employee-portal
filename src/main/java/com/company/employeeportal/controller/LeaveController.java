package com.company.employeeportal.controller;

import com.company.employeeportal.dto.LeaveApprovalRequest;
import com.company.employeeportal.dto.LeaveRequest;
import com.company.employeeportal.dto.LeaveResponse;
import com.company.employeeportal.security.UserPrincipal;
import com.company.employeeportal.service.LeaveService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for leave management operations.
 * Handles leave requests, approvals, and history retrieval.
 */
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    private final LeaveService leaveService;

    @Autowired
    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Submit a new leave request.
     */
    @PostMapping("/request")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<LeaveResponse> submitLeaveRequest(
            @Valid @RequestBody LeaveRequest leaveRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Leave request submission by user: {}", userPrincipal.getUsername());
        
        LeaveResponse response = leaveService.createLeaveRequest(leaveRequest, userPrincipal.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get current user's leave history.
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<List<LeaveResponse>> getMyLeaveRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Retrieving leave history for user: {}", userPrincipal.getUsername());
        
        List<LeaveResponse> leaves = leaveService.getUserLeaveHistory(userPrincipal.getId());
        
        return ResponseEntity.ok(leaves);
    }

    /**
     * Get current user's leave history with pagination.
     */
    @GetMapping("/my-requests/paginated")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<Page<LeaveResponse>> getMyLeaveRequestsPaginated(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Retrieving paginated leave history for user: {}", userPrincipal.getUsername());
        
        Page<LeaveResponse> leaves = leaveService.getUserLeaveHistory(userPrincipal.getId(), pageable);
        
        return ResponseEntity.ok(leaves);
    }

    /**
     * Get a specific leave request by ID.
     */
    @GetMapping("/{leaveId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<LeaveResponse> getLeaveRequest(
            @PathVariable Long leaveId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Retrieving leave request ID: {} for user: {}", leaveId, userPrincipal.getUsername());
        
        LeaveResponse leave = leaveService.getLeaveById(leaveId, userPrincipal.getId());
        
        return ResponseEntity.ok(leave);
    }

    /**
     * Get pending leave requests for HR approval.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<LeaveResponse>> getPendingLeaveRequests() {
        
        logger.debug("Retrieving pending leave requests for manager approval");
        
        List<LeaveResponse> pendingLeaves = leaveService.getPendingLeaveRequests();
        
        return ResponseEntity.ok(pendingLeaves);
    }

    /**
     * Get pending leave requests with pagination.
     */
    @GetMapping("/pending/paginated")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Page<LeaveResponse>> getPendingLeaveRequestsPaginated(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Retrieving paginated pending leave requests for manager approval");
        
        Page<LeaveResponse> pendingLeaves = leaveService.getPendingLeaveRequests(pageable);
        
        return ResponseEntity.ok(pendingLeaves);
    }

    /**
     * Approve a leave request.
     */
    @PutMapping("/{leaveId}/approve")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<LeaveResponse> approveLeaveRequest(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveApprovalRequest approvalRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Approving leave request ID: {} by manager: {}", leaveId, userPrincipal.getUsername());
        
        LeaveResponse response = leaveService.approveLeaveRequest(leaveId, approvalRequest, userPrincipal.getId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a leave request.
     */
    @PutMapping("/{leaveId}/reject")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<LeaveResponse> rejectLeaveRequest(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveApprovalRequest approvalRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Rejecting leave request ID: {} by manager: {}", leaveId, userPrincipal.getUsername());
        
        LeaveResponse response = leaveService.rejectLeaveRequest(leaveId, approvalRequest, userPrincipal.getId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get approved leaves within a date range (for calendar view).
     */
    @GetMapping("/calendar")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<List<LeaveResponse>> getApprovedLeavesForCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Retrieving approved leaves for calendar view between {} and {}", startDate, endDate);
        
        List<LeaveResponse> approvedLeaves = leaveService.getApprovedLeavesInDateRange(startDate, endDate);
        
        return ResponseEntity.ok(approvedLeaves);
    }

    /**
     * Get leave statistics for current user.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<LeaveService.LeaveStatistics> getLeaveStatistics(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Retrieving leave statistics for user: {} for year: {}", userPrincipal.getUsername(), year);
        
        LeaveService.LeaveStatistics statistics = leaveService.getUserLeaveStatistics(userPrincipal.getId(), year);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get all leave requests for HR management.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<LeaveResponse>> getAllLeaveRequests() {
        
        logger.debug("Retrieving all leave requests for HR management");
        
        List<LeaveResponse> allLeaves = leaveService.getAllLeaveRequests();
        
        return ResponseEntity.ok(allLeaves);
    }

    /**
     * Get all leave requests with pagination for HR management.
     */
    @GetMapping("/all/paginated")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Page<LeaveResponse>> getAllLeaveRequestsPaginated(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Retrieving paginated all leave requests for HR management");
        
        Page<LeaveResponse> allLeaves = leaveService.getAllLeaveRequests(pageable);
        
        return ResponseEntity.ok(allLeaves);
    }

    /**
     * Health check endpoint for leave service.
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Leave service is running");
    }
}
