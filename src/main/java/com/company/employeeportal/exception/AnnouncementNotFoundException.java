package com.company.employeeportal.exception;

/**
 * Exception thrown when an announcement is not found in the system.
 */
public class AnnouncementNotFoundException extends RuntimeException {

    public AnnouncementNotFoundException(String message) {
        super(message);
    }

    public AnnouncementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
