# User Input Validation Implementation

This document describes the comprehensive user input validation system implemented for the Employee Portal application.

## Overview

The validation system implements multiple layers of validation to ensure data integrity, security, and compliance with business rules. It includes:

1. **Custom Validators** for business-specific validation rules
2. **Email Uniqueness Validation** with support for user updates
3. **Password Strength Requirements** with configurable complexity rules
4. **Bean Validation** with comprehensive error messages
5. **Input Sanitization** to prevent XSS attacks

## Custom Validators

### 1. ValidName Validator

**Purpose**: Validates that names contain only appropriate characters for human names.

**Annotation**: `@ValidName`

**Rules**:
- Allows letters (including Unicode for international names)
- Allows spaces, hyphens, and apostrophes
- Prevents names starting/ending with special characters
- Prevents consecutive special characters
- Prevents excessive consecutive spaces

**Usage**:
```java
@ValidName(allowEmpty = false)
private String name;
```

### 2. ValidContactInfo Validator

**Purpose**: Validates contact information formats including phone numbers and emails.

**Annotation**: `@ValidContactInfo`

**Rules**:
- Supports phone number formats: `123-456-7890`, `(123) 456-7890`, `+1-123-456-7890`
- Supports email formats: `user@domain.com`
- Supports mixed formats: `123-456-7890, user@domain.com`
- Validates each component in mixed formats

**Usage**:
```java
@ValidContactInfo
private String contactInfo;
```

### 3. StrongPassword Validator

**Purpose**: Enforces password complexity requirements for security.

**Annotation**: `@StrongPassword`

**Rules** (configurable):
- Minimum length (default: 8 characters)
- Requires uppercase letters (configurable)
- Requires lowercase letters (configurable)
- Requires digits (configurable)
- Requires special characters (configurable)

**Usage**:
```java
@StrongPassword(minLength = 8, requireUppercase = true, requireLowercase = true, requireDigits = true, requireSpecialChars = true)
private String password;
```

### 4. PasswordMatches Validator

**Purpose**: Ensures password and confirm password fields match.

**Annotation**: `@PasswordMatches` (class-level)

**Rules**:
- Compares two fields using reflection
- Configurable field names
- Handles null values appropriately

**Usage**:
```java
@PasswordMatches(password = "newPassword", confirmPassword = "confirmPassword")
public class ChangePasswordRequest {
    // fields...
}
```

## Email Uniqueness Validation

### 1. UniqueEmail Validator

**Purpose**: Ensures email addresses are unique across all users.

**Annotation**: `@UniqueEmail`

**Usage**: For new user creation
```java
@UniqueEmail
private String email;
```

### 2. UniqueEmailForUpdate Validator

**Purpose**: Ensures email uniqueness while allowing users to keep their current email during updates.

**Annotation**: `@UniqueEmailForUpdate` (class-level)

**Rules**:
- Checks email uniqueness in database
- Excludes current user's ID from uniqueness check
- Uses reflection to access email and userId fields

**Usage**:
```java
@UniqueEmailForUpdate(emailField = "email", userIdField = "userId")
public class UserProfileUpdateRequest {
    // fields...
}
```

## Bean Validation Configuration

### Error Message Handling

The `GlobalExceptionHandler` provides comprehensive error handling for validation failures:

1. **Field-level validation errors**: Mapped to specific field names
2. **Class-level validation errors**: Mapped to general validation category
3. **Constraint violation exceptions**: Handled for programmatic validation
4. **Structured error responses**: Consistent JSON format for all validation errors

### Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users/profile",
  "validationErrors": {
    "name": "Name must contain only letters, spaces, hyphens, and apostrophes",
    "password": "Password must contain at least one uppercase letter",
    "email": "Email already exists"
  }
}
```

## Input Sanitization

### XSS Prevention

The `UserService` includes input sanitization to prevent XSS attacks:

```java
private String sanitizeInput(String input) {
    if (!StringUtils.hasText(input)) {
        return input;
    }
    
    return input.replaceAll("<", "&lt;")
               .replaceAll(">", "&gt;")
               .replaceAll("\"", "&quot;")
               .replaceAll("'", "&#x27;")
               .replaceAll("/", "&#x2F;");
}
```

## Validation Groups and Scenarios

### User Creation Validation

- All fields validated according to their annotations
- Email uniqueness checked against entire database
- Password strength enforced
- Name format validated

### User Profile Update Validation

- Uses `UserProfileUpdateRequest` DTO
- Email uniqueness excludes current user
- Optional fields validated only if provided
- Admin users can update additional fields

### Password Change Validation

- Current password verification (for non-admin users)
- New password strength validation
- Password confirmation matching
- Different validation rules for admin password resets

## Configuration

### ValidationConfig

Enables method-level validation and configures the validator factory:

```java
@Configuration
public class ValidationConfig {
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        // Configuration...
    }
    
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}
```

## Testing

### Validation Tests

The `ValidationTest` class provides comprehensive testing for all custom validators:

- Valid input scenarios
- Invalid input scenarios
- Edge cases and boundary conditions
- Integration with Spring Boot validation framework

## Security Considerations

1. **Input Sanitization**: All user inputs are sanitized to prevent XSS attacks
2. **SQL Injection Prevention**: Parameterized queries used throughout
3. **Password Security**: Strong password requirements enforced
4. **Email Validation**: Proper email format validation prevents malformed data
5. **Length Limits**: All fields have appropriate length restrictions

## Performance Considerations

1. **Database Queries**: Email uniqueness checks are optimized with database indexes
2. **Validation Caching**: Validator instances are cached by Spring
3. **Reflection Usage**: Minimal reflection usage in class-level validators
4. **Error Handling**: Efficient error collection and formatting

## Requirements Compliance

This implementation satisfies the following requirements:

- **Requirement 2.4**: Input validation and sanitization implemented
- **Requirement 8.2**: Comprehensive validation prevents injection attacks
- **Security**: Password strength and email validation enhance security
- **User Experience**: Clear, structured error messages for validation failures