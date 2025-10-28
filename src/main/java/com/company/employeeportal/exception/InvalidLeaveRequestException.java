package com.company.employeeportal.exception;

/**
 * Exception thrown when a leave request is invalid or violates business rules.
 */
public class InvalidLeaveRequestException extends RuntimeException {

    public InvalidLeaveRequestException(String message) {
        super(message);
    }

    public InvalidLeaveRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}