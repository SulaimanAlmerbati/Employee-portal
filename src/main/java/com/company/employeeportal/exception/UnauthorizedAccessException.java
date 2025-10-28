package com.company.employeeportal.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}