package com.company.employeeportal.exception;

/**
 * Exception thrown when a user attempts to access payroll data they are not authorized to view.
 * This exception is used to enforce access control for payroll information.
 */
public class PayrollAccessException extends RuntimeException {

    public PayrollAccessException(String message) {
        super(message);
    }

    public PayrollAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
