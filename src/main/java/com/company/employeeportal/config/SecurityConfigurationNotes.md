# Spring Security Configuration

## Overview
This security configuration implements JWT-based authentication with role-based access control for the Employee Portal application.

## Key Features Implemented

### 1. JWT Authentication Filter Integration
- **JwtAuthenticationFilter** is integrated before UsernamePasswordAuthenticationFilter
- Validates JWT tokens from Authorization header
- Sets authentication context for valid tokens
- Handles token blacklisting for logout functionality

### 2. Role-Based Access Control (Requirement 1.5)
- **Employee Role**: Access to personal profile, leave requests, payroll, and announcements
- **Manager Role**: All employee permissions + leave approval/rejection capabilities
- **Admin Role**: All manager permissions + user management and announcement creation
- Hierarchical role system where higher roles inherit lower role permissions

### 3. CORS Configuration (Frontend Integration)
- Configured for frontend integration with specific allowed headers
- Supports credentials for JWT token handling
- Preflight request caching for performance
- Configurable allowed origins via properties

### 4. CSRF Protection (Disabled for Stateless API)
- CSRF disabled as the API is stateless using JWT tokens
- Appropriate for REST API architecture
- Prevents CSRF attacks through token-based authentication

### 5. Security Headers (Requirement 8.1, 8.2)
- **X-Content-Type-Options**: Prevents MIME type sniffing attacks
- **X-Frame-Options**: Prevents clickjacking attacks
- **HSTS**: Enforces HTTPS in production environments
- Frame options configured for H2 console in development

### 6. HTTPS Enforcement (Requirement 8.1)
- Configurable HTTPS requirement via `app.security.require-https` property
- Enforces secure channels in production environments
- Supports both development and production configurations

### 7. Session Management
- Stateless session creation policy for JWT-based authentication
- No server-side session storage
- Token-based authentication aligns with REST API principles

## Endpoint Security Mapping

### Public Endpoints
- `/api/auth/**` - Authentication endpoints
- `/actuator/health` - Health check
- Static resources (`/css/**`, `/js/**`, `/images/**`)
- Login page and root path

### Employee Endpoints (EMPLOYEE, MANAGER, ADMIN)
- `/api/users/profile` - User profile management
- `/api/leaves/my-requests` - Personal leave requests
- `/api/leaves/request` - Leave request submission
- `/api/payroll/my-payslips` - Personal payroll data
- `/api/announcements` - Company announcements

### Manager Endpoints (MANAGER, ADMIN)
- `/api/leaves/pending` - Pending leave requests
- `/api/leaves/*/approve` - Leave approval
- `/api/leaves/*/reject` - Leave rejection

### Admin Endpoints (ADMIN only)
- `/api/users/**` - User management
- `/api/announcements/**` - Announcement management
- `/api/admin/**` - Administrative functions

## Configuration Properties

### Security Properties
```properties
app.security.require-https=false                    # HTTPS enforcement
app.security.cors.allowed-origins=http://localhost:3000,http://localhost:8080
app.security.cors.max-age=3600                     # CORS preflight cache
app.security.session-timeout-minutes=30            # Session timeout
```

### JWT Properties
```properties
app.jwt.expiration=3600000                          # 1 hour token expiration
app.jwt.session-timeout-minutes=30                  # Session timeout
```

## Security Requirements Compliance

### Requirement 1.5: Role-Based Access Control
✅ **Implemented**: Comprehensive role-based access control with hierarchical permissions

### Requirement 8.1: HTTPS Communication
✅ **Implemented**: HTTPS enforcement configurable via properties, HSTS headers

### Requirement 8.2: Input Validation and Security
✅ **Implemented**: Security headers, CORS configuration, JWT validation

## Testing
- Comprehensive security tests in `SecurityConfigTest.java`
- JWT authentication integration tests
- Role-based access control verification
- CORS and security header validation

## Notes
- H2 console access is configured for development environments only
- Production deployments should set `app.security.require-https=true`
- CORS origins should be restricted in production environments
- JWT token expiration aligns with session timeout requirements