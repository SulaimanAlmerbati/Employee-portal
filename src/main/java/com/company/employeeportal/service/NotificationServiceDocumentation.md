# Notification Service Implementation

## Overview

The NotificationService provides email notification functionality for leave request status changes. It implements async processing using Spring's `@Async` annotation and uses Thymeleaf templates for professional HTML email formatting.

## Features Implemented

### 1. Email Notification for Status Changes
- **Leave Request Submitted**: Notifies employee when their request is submitted
- **Leave Request Approved**: Notifies employee when their request is approved
- **Leave Request Rejected**: Notifies employee when their request is rejected
- **Manager Notification**: Notifies all managers/admins when a new leave request is submitted

### 2. Notification Templates
Professional HTML email templates created for different scenarios:
- `leave-submitted.html`: Confirmation email for submitted requests
- `leave-approved.html`: Approval notification with celebration styling
- `leave-rejected.html`: Rejection notification with next steps
- `leave-manager-notification.html`: Manager notification for pending approvals

### 3. Async Processing
- All notification methods use `@Async("notificationTaskExecutor")` for non-blocking execution
- Dedicated thread pool configured in `AsyncConfig` for notification processing
- Prevents email sending delays from blocking the main application flow

## Technical Implementation

### Dependencies
- **JavaMailSender**: Spring Boot's email sending capability
- **TemplateEngine**: Thymeleaf for HTML template processing
- **UserRepository**: To fetch managers for notifications

### Configuration Properties
```properties
# Email server configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-password

# Notification customization
app.notification.company-name=Employee Portal
app.notification.portal-url=http://localhost:8080
```

### Template Context Variables
Each email template receives a context with:
- User information (name, email, department)
- Leave details (type, dates, duration, reason)
- Manager information (when applicable)
- System information (company name, portal URL)

### Error Handling
- Template processing failures fall back to plain text content
- Email sending errors are logged but don't block the application
- Graceful degradation ensures notifications are always attempted

## Integration Points

### LeaveService Integration
The NotificationService is automatically called by LeaveService during:
- `createLeaveRequest()`: Sends submission confirmation and manager notification
- `approveLeaveRequest()`: Sends approval notification to employee
- `rejectLeaveRequest()`: Sends rejection notification to employee

### Manager Notifications
- Automatically finds all active users with MANAGER or ADMIN roles
- Sends notification to all managers when new leave requests are submitted
- Includes employee details and direct link to review the request

## Testing

### Unit Tests
- Comprehensive test coverage in `NotificationServiceTest`
- Mocks email dependencies for isolated testing
- Verifies template processing and email sending calls
- Tests error scenarios and fallback behavior

### Integration with LeaveService
- `LeaveServiceTest` verifies notification service calls
- Ensures notifications are sent at appropriate times
- Validates that failed notifications don't break leave processing

## Future Enhancements

### Potential Improvements
1. **Email Templates**: Add more template variations for different leave types
2. **Notification Preferences**: Allow users to configure notification preferences
3. **SMS Notifications**: Add SMS capability for urgent notifications
4. **Digest Emails**: Send daily/weekly digest emails to managers
5. **Reminder Notifications**: Send reminders for pending approvals

### Configuration Enhancements
1. **Template Customization**: Allow runtime template customization
2. **Notification Scheduling**: Add scheduling for non-urgent notifications
3. **Retry Logic**: Implement retry mechanism for failed email sends
4. **Delivery Tracking**: Add email delivery confirmation tracking

## Requirements Satisfied

This implementation satisfies the following requirements from the specification:

- **Requirement 3.5**: "WHEN leave request status changes, THE Employee Portal System SHALL notify the requesting employee"
- **Requirement 7.3**: "WHEN a manager approves a leave request, THE Employee Portal System SHALL update the status and notify the employee"

The async processing ensures that email sending doesn't impact application performance, and the template system provides professional, branded communications to users.