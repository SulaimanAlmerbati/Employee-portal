package com.company.employeeportal.dto;

import com.company.employeeportal.model.LeaveStatus;
import com.company.employeeportal.model.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for leave response data.
 */
public class LeaveResponse {

    private Long id;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private String managerComments;
    private String approvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long durationInDays;

    // Constructors
    public LeaveResponse() {}

    public LeaveResponse(Long id, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
                        String reason, LeaveStatus status, String managerComments,
                        String approvedByName, LocalDateTime createdAt, LocalDateTime updatedAt,
                        long durationInDays) {
        this.id = id;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
        this.managerComments = managerComments;
        this.approvedByName = approvedByName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.durationInDays = durationInDays;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getManagerComments() {
        return managerComments;
    }

    public void setManagerComments(String managerComments) {
        this.managerComments = managerComments;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getDurationInDays() {
        return durationInDays;
    }

    public void setDurationInDays(long durationInDays) {
        this.durationInDays = durationInDays;
    }

    @Override
    public String toString() {
        return "LeaveResponse{" +
                "id=" + id +
                ", leaveType=" + leaveType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", durationInDays=" + durationInDays +
                '}';
    }
}