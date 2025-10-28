package com.company.employeeportal.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for leave approval/rejection requests.
 */
public class LeaveApprovalRequest {

    @Size(max = 500, message = "Comments must not exceed 500 characters")
    private String comments;

    // Constructors
    public LeaveApprovalRequest() {}

    public LeaveApprovalRequest(String comments) {
        this.comments = comments;
    }

    // Getters and Setters
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "LeaveApprovalRequest{" +
                "comments='" + comments + '\'' +
                '}';
    }
}