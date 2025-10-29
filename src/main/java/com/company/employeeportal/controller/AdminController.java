package com.company.employeeportal.controller;

import com.company.employeeportal.dto.DashboardMetrics;
import com.company.employeeportal.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for admin dashboard functionality.
 * Provides endpoints for dashboard metrics, user statistics, and system monitoring.
 * All endpoints require ADMIN role access.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('IT_ADMIN') or hasRole('HR')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Get comprehensive dashboard metrics.
     * 
     * @return ResponseEntity containing dashboard metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetrics> getDashboardMetrics() {
        logger.info("Admin dashboard metrics requested");
        
        try {
            DashboardMetrics metrics = adminService.getDashboardMetrics();
            logger.info("Dashboard metrics retrieved successfully");
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Error retrieving dashboard metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get detailed user statistics.
     * 
     * @return ResponseEntity containing user statistics
     */
    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        logger.info("User statistics requested");
        
        try {
            Map<String, Object> stats = adminService.getUserStatistics();
            logger.info("User statistics retrieved successfully");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error retrieving user statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get system monitoring data.
     * 
     * @return ResponseEntity containing system monitoring information
     */
    @GetMapping("/system/monitoring")
    public ResponseEntity<Map<String, Object>> getSystemMonitoring() {
        logger.info("System monitoring data requested");
        
        try {
            Map<String, Object> monitoringData = adminService.getSystemMonitoringData();
            logger.info("System monitoring data retrieved successfully");
            return ResponseEntity.ok(monitoringData);
            
        } catch (Exception e) {
            logger.error("Error retrieving system monitoring data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get system health status.
     * 
     * @return ResponseEntity containing system health information
     */
    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        logger.info("System health check requested");
        
        try {
            Map<String, Object> healthData = adminService.getSystemMonitoringData();
            
            // Extract health-specific information
            Map<String, Object> healthStatus = Map.of(
                "status", healthData.getOrDefault("systemStatus", "UNKNOWN"),
                "databaseStatus", healthData.getOrDefault("databaseStatus", "UNKNOWN"),
                "lastChecked", healthData.getOrDefault("lastChecked", "UNKNOWN")
            );
            
            logger.info("System health check completed");
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            logger.error("Error during system health check", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "ERROR", "message", "Health check failed"));
        }
    }

    /**
     * Get pending leave requests count (quick metric for dashboard).
     * 
     * @return ResponseEntity containing pending leave count
     */
    @GetMapping("/leaves/pending-count")
    public ResponseEntity<Map<String, Object>> getPendingLeavesCount() {
        logger.info("Pending leaves count requested");
        
        try {
            DashboardMetrics metrics = adminService.getDashboardMetrics();
            
            Map<String, Object> response = Map.of(
                "pendingCount", metrics.getPendingLeaveRequests(),
                "totalLeaves", metrics.getTotalLeaveRequests(),
                "approvedCount", metrics.getApprovedLeaveRequests(),
                "rejectedCount", metrics.getRejectedLeaveRequests()
            );
            
            logger.info("Pending leaves count retrieved: {}", metrics.getPendingLeaveRequests());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving pending leaves count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user count by role (quick metric for dashboard).
     * 
     * @return ResponseEntity containing user counts by role
     */
    @GetMapping("/users/role-counts")
    public ResponseEntity<Map<String, Object>> getUserRoleCounts() {
        logger.info("User role counts requested");
        
        try {
            Map<String, Object> stats = adminService.getUserStatistics();
            
            @SuppressWarnings("unchecked")
            Map<String, Long> roleBreakdown = (Map<String, Long>) stats.get("roleBreakdown");
            
            Map<String, Object> response = Map.of(
                "employees", roleBreakdown.getOrDefault("EMPLOYEE", 0L),
                "managers", roleBreakdown.getOrDefault("MANAGER", 0L),
                "admins", roleBreakdown.getOrDefault("ADMIN", 0L),
                "totalActive", stats.getOrDefault("activeUsers", 0L)
            );
            
            logger.info("User role counts retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving user role counts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get department statistics (user distribution by department).
     * 
     * @return ResponseEntity containing department statistics
     */
    @GetMapping("/users/department-stats")
    public ResponseEntity<Map<String, Object>> getDepartmentStatistics() {
        logger.info("Department statistics requested");
        
        try {
            Map<String, Object> stats = adminService.getUserStatistics();
            
            @SuppressWarnings("unchecked")
            Map<String, Long> departmentBreakdown = (Map<String, Long>) stats.get("departmentBreakdown");
            
            Map<String, Object> response = Map.of(
                "departmentBreakdown", departmentBreakdown,
                "totalDepartments", departmentBreakdown.size(),
                "largestDepartment", findLargestDepartment(departmentBreakdown)
            );
            
            logger.info("Department statistics retrieved for {} departments", departmentBreakdown.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving department statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to find the department with the most users.
     */
    private Map<String, Object> findLargestDepartment(Map<String, Long> departmentBreakdown) {
        if (departmentBreakdown.isEmpty()) {
            return Map.of("name", "None", "count", 0L);
        }
        
        Map.Entry<String, Long> largest = departmentBreakdown.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("None", 0L));
        
        return Map.of("name", largest.getKey(), "count", largest.getValue());
    }
}