package com.company.employeeportal.exception;

/**
 * Exception thrown when leave approval/rejection fails.
 */
public class LeaveApprovalException extends RuntimeException {

    public LeaveApprovalException(String message) {
        super(message);
    }

    public LeaveApprovalException(String message, Throwable cause) {
        super(message, cause);
    }
}