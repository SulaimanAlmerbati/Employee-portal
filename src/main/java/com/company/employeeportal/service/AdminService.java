package com.company.employeeportal.service;

import com.company.employeeportal.dto.DashboardMetrics;
import com.company.employeeportal.model.LeaveStatus;
import com.company.employeeportal.model.LeaveType;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.repository.AnnouncementRepository;
import com.company.employeeportal.repository.LeaveRepository;
import com.company.employeeportal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for admin dashboard functionality.
 * Provides metrics calculation and system monitoring data.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final LeaveRepository leaveRepository;
    private final AnnouncementRepository announcementRepository;

    @Autowired
    public AdminService(UserRepository userRepository,
                       LeaveRepository leaveRepository,
                       AnnouncementRepository announcementRepository) {
        this.userRepository = userRepository;
        this.leaveRepository = leaveRepository;
        this.announcementRepository = announcementRepository;
    }

    /**
     * Calculate and return comprehensive dashboard metrics.
     * 
     * @return DashboardMetrics containing all system statistics
     */
    public DashboardMetrics getDashboardMetrics() {
        logger.info("Calculating dashboard metrics");
        
        DashboardMetrics metrics = new DashboardMetrics();
        
        try {
            // User statistics
            calculateUserMetrics(metrics);
            
            // Leave request statistics
            calculateLeaveMetrics(metrics);
            
            // Announcement statistics
            calculateAnnouncementMetrics(metrics);
            
            // Department breakdown
            calculateDepartmentMetrics(metrics);
            
            // Leave type breakdown
            calculateLeaveTypeMetrics(metrics);
            
            // Leave status breakdown
            calculateLeaveStatusMetrics(metrics);
            
            // System health
            calculateSystemHealth(metrics);
            
            logger.info("Dashboard metrics calculated successfully");
            
        } catch (Exception e) {
            logger.error("Error calculating dashboard metrics", e);
            metrics.setSystemStatus("ERROR");
        }
        
        return metrics;
    }

    /**
     * Calculate user-related metrics.
     */
    private void calculateUserMetrics(DashboardMetrics metrics) {
        metrics.setTotalUsers(userRepository.count());
        metrics.setActiveUsers(userRepository.countByActiveTrue());
        metrics.setTotalEmployees(userRepository.countByRoleAndActiveTrue(Role.EMPLOYEE));
        metrics.setTotalManagers(userRepository.countByRoleAndActiveTrue(Role.MANAGER));
        metrics.setTotalAdmins(userRepository.countByRoleAndActiveTrue(Role.ADMIN));
        
        logger.debug("User metrics calculated: {} total users, {} active users", 
                    metrics.getTotalUsers(), metrics.getActiveUsers());
    }

    /**
     * Calculate leave request metrics.
     */
    private void calculateLeaveMetrics(DashboardMetrics metrics) {
        metrics.setTotalLeaveRequests(leaveRepository.count());
        metrics.setPendingLeaveRequests(leaveRepository.countByStatus(LeaveStatus.PENDING));
        metrics.setApprovedLeaveRequests(leaveRepository.countByStatus(LeaveStatus.APPROVED));
        metrics.setRejectedLeaveRequests(leaveRepository.countByStatus(LeaveStatus.REJECTED));
        
        logger.debug("Leave metrics calculated: {} total leaves, {} pending", 
                    metrics.getTotalLeaveRequests(), metrics.getPendingLeaveRequests());
    }

    /**
     * Calculate announcement metrics.
     */
    private void calculateAnnouncementMetrics(DashboardMetrics metrics) {
        metrics.setTotalAnnouncements(announcementRepository.count());
        metrics.setActiveAnnouncements(announcementRepository.countByActiveTrue());
        
        logger.debug("Announcement metrics calculated: {} total, {} active", 
                    metrics.getTotalAnnouncements(), metrics.getActiveAnnouncements());
    }

    /**
     * Calculate department breakdown metrics.
     */
    private void calculateDepartmentMetrics(DashboardMetrics metrics) {
        Map<String, Long> departmentCounts = new HashMap<>();
        
        // Get all active users and group by department
        userRepository.findByActiveTrue().forEach(user -> {
            String department = user.getDepartment() != null ? user.getDepartment() : "Unassigned";
            departmentCounts.merge(department, 1L, Long::sum);
        });
        
        metrics.setUsersByDepartment(departmentCounts);
        
        logger.debug("Department metrics calculated for {} departments", departmentCounts.size());
    }

    /**
     * Calculate leave type breakdown metrics.
     */
    private void calculateLeaveTypeMetrics(DashboardMetrics metrics) {
        Map<String, Long> leaveTypeCounts = new HashMap<>();
        
        // Initialize with all leave types
        for (LeaveType type : LeaveType.values()) {
            leaveTypeCounts.put(type.name(), leaveRepository.countByLeaveType(type));
        }
        
        metrics.setLeavesByType(leaveTypeCounts);
        
        logger.debug("Leave type metrics calculated for {} types", leaveTypeCounts.size());
    }

    /**
     * Calculate leave status breakdown metrics.
     */
    private void calculateLeaveStatusMetrics(DashboardMetrics metrics) {
        Map<String, Long> leaveStatusCounts = new HashMap<>();
        
        // Initialize with all leave statuses
        for (LeaveStatus status : LeaveStatus.values()) {
            leaveStatusCounts.put(status.name(), leaveRepository.countByStatus(status));
        }
        
        metrics.setLeavesByStatus(leaveStatusCounts);
        
        logger.debug("Leave status metrics calculated for {} statuses", leaveStatusCounts.size());
    }

    /**
     * Calculate system health metrics.
     */
    private void calculateSystemHealth(DashboardMetrics metrics) {
        try {
            // Basic health checks
            boolean dbHealthy = checkDatabaseHealth();
            boolean systemHealthy = checkSystemHealth();
            
            if (dbHealthy && systemHealthy) {
                metrics.setSystemStatus("HEALTHY");
            } else if (dbHealthy) {
                metrics.setSystemStatus("DEGRADED");
            } else {
                metrics.setSystemStatus("UNHEALTHY");
            }
            
            metrics.setLastUpdated(LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error checking system health", e);
            metrics.setSystemStatus("ERROR");
        }
    }

    /**
     * Check database connectivity and basic operations.
     */
    private boolean checkDatabaseHealth() {
        try {
            // Simple query to check database connectivity
            userRepository.count();
            leaveRepository.count();
            announcementRepository.count();
            return true;
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }

    /**
     * Check overall system health.
     */
    private boolean checkSystemHealth() {
        try {
            // Check if we have at least one admin user
            long adminCount = userRepository.countByRoleAndActiveTrue(Role.ADMIN);
            if (adminCount == 0) {
                logger.warn("No active admin users found");
                return false;
            }
            
            // Additional system checks can be added here
            return true;
            
        } catch (Exception e) {
            logger.error("System health check failed", e);
            return false;
        }
    }

    /**
     * Get user statistics summary.
     * 
     * @return Map containing user statistics
     */
    public Map<String, Object> getUserStatistics() {
        logger.info("Calculating user statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalUsers", userRepository.count());
            stats.put("activeUsers", userRepository.countByActiveTrue());
            stats.put("inactiveUsers", userRepository.count() - userRepository.countByActiveTrue());
            
            // Role breakdown
            Map<String, Long> roleBreakdown = new HashMap<>();
            roleBreakdown.put("EMPLOYEE", userRepository.countByRoleAndActiveTrue(Role.EMPLOYEE));
            roleBreakdown.put("MANAGER", userRepository.countByRoleAndActiveTrue(Role.MANAGER));
            roleBreakdown.put("ADMIN", userRepository.countByRoleAndActiveTrue(Role.ADMIN));
            stats.put("roleBreakdown", roleBreakdown);
            
            // Department breakdown
            Map<String, Long> departmentBreakdown = new HashMap<>();
            userRepository.findByActiveTrue().forEach(user -> {
                String department = user.getDepartment() != null ? user.getDepartment() : "Unassigned";
                departmentBreakdown.merge(department, 1L, Long::sum);
            });
            stats.put("departmentBreakdown", departmentBreakdown);
            
            logger.info("User statistics calculated successfully");
            
        } catch (Exception e) {
            logger.error("Error calculating user statistics", e);
            stats.put("error", "Failed to calculate user statistics");
        }
        
        return stats;
    }

    /**
     * Get system monitoring data.
     * 
     * @return Map containing system monitoring information
     */
    public Map<String, Object> getSystemMonitoringData() {
        logger.info("Gathering system monitoring data");
        
        Map<String, Object> monitoringData = new HashMap<>();
        
        try {
            // Database status
            monitoringData.put("databaseStatus", checkDatabaseHealth() ? "HEALTHY" : "UNHEALTHY");
            
            // System uptime (simplified - in a real system this would be more sophisticated)
            monitoringData.put("systemStatus", checkSystemHealth() ? "HEALTHY" : "DEGRADED");
            
            // Recent activity metrics
            monitoringData.put("pendingLeaves", leaveRepository.countByStatus(LeaveStatus.PENDING));
            monitoringData.put("activeAnnouncements", announcementRepository.countByActiveTrue());
            
            // Timestamp
            monitoringData.put("lastChecked", LocalDateTime.now());
            
            logger.info("System monitoring data gathered successfully");
            
        } catch (Exception e) {
            logger.error("Error gathering system monitoring data", e);
            monitoringData.put("error", "Failed to gather monitoring data");
            monitoringData.put("systemStatus", "ERROR");
        }
        
        return monitoringData;
    }
}