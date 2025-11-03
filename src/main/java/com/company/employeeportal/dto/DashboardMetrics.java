package com.company.employeeportal.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for admin dashboard metrics and statistics.
 * Contains aggregated data for system monitoring and reporting.
 */
public class DashboardMetrics {

    private long totalUsers;
    private long activeUsers;
    private long totalEmployees;
    private long totalManagers;
    private long totalAdmins;
    
    private long totalLeaveRequests;
    private long pendingLeaveRequests;
    private long approvedLeaveRequests;
    private long rejectedLeaveRequests;
    
    private long totalAnnouncements;
    private long activeAnnouncements;
    
    private Map<String, Long> usersByDepartment;
    private Map<String, Long> leavesByType;
    private Map<String, Long> leavesByStatus;
    
    private LocalDateTime lastUpdated;
    private String systemStatus;

    // Constructors
    public DashboardMetrics() {
        this.lastUpdated = LocalDateTime.now();
        this.systemStatus = "HEALTHY";
    }

    // Getters and Setters
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getTotalManagers() {
        return totalManagers;
    }

    public void setTotalManagers(long totalManagers) {
        this.totalManagers = totalManagers;
    }

    public long getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(long totalAdmins) {
        this.totalAdmins = totalAdmins;
    }

    public long getTotalLeaveRequests() {
        return totalLeaveRequests;
    }

    public void setTotalLeaveRequests(long totalLeaveRequests) {
        this.totalLeaveRequests = totalLeaveRequests;
    }

    public long getPendingLeaveRequests() {
        return pendingLeaveRequests;
    }

    public void setPendingLeaveRequests(long pendingLeaveRequests) {
        this.pendingLeaveRequests = pendingLeaveRequests;
    }

    public long getApprovedLeaveRequests() {
        return approvedLeaveRequests;
    }

    public void setApprovedLeaveRequests(long approvedLeaveRequests) {
        this.approvedLeaveRequests = approvedLeaveRequests;
    }

    public long getRejectedLeaveRequests() {
        return rejectedLeaveRequests;
    }

    public void setRejectedLeaveRequests(long rejectedLeaveRequests) {
        this.rejectedLeaveRequests = rejectedLeaveRequests;
    }

    public long getTotalAnnouncements() {
        return totalAnnouncements;
    }

    public void setTotalAnnouncements(long totalAnnouncements) {
        this.totalAnnouncements = totalAnnouncements;
    }

    public long getActiveAnnouncements() {
        return activeAnnouncements;
    }

    public void setActiveAnnouncements(long activeAnnouncements) {
        this.activeAnnouncements = activeAnnouncements;
    }

    public Map<String, Long> getUsersByDepartment() {
        return usersByDepartment;
    }

    public void setUsersByDepartment(Map<String, Long> usersByDepartment) {
        this.usersByDepartment = usersByDepartment;
    }

    public Map<String, Long> getLeavesByType() {
        return leavesByType;
    }

    public void setLeavesByType(Map<String, Long> leavesByType) {
        this.leavesByType = leavesByType;
    }

    public Map<String, Long> getLeavesByStatus() {
        return leavesByStatus;
    }

    public void setLeavesByStatus(Map<String, Long> leavesByStatus) {
        this.leavesByStatus = leavesByStatus;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(String systemStatus) {
        this.systemStatus = systemStatus;
    }

    @Override
    public String toString() {
        return "DashboardMetrics{" +
                "totalUsers=" + totalUsers +
                ", activeUsers=" + activeUsers +
                ", pendingLeaveRequests=" + pendingLeaveRequests +
                ", systemStatus='" + systemStatus + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
