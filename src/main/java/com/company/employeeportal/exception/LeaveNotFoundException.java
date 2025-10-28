package com.company.employeeportal.exception;

/**
 * Exception thrown when a leave request is not found.
 */
public class LeaveNotFoundException extends RuntimeException {

    public LeaveNotFoundException(String message) {
        super(message);
    }

    public LeaveNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeaveNotFoundException(Long leaveId) {
        super("Leave request not found with id: " + leaveId);
    }
}