/**
 * JWT Authentication and Security Package
 * 
 * This package contains all JWT-related security components for the Employee Portal application.
 * 
 * Key Components:
 * 
 * 1. JwtUtil - Core JWT utility class for token generation, validation, and parsing
 *    - Generates access and refresh tokens
 *    - Validates token structure and expiration
 *    - Extracts claims and user information from tokens
 *    - Supports token refresh functionality
 * 
 * 2. JwtAuthenticationFilter - Spring Security filter for JWT token processing
 *    - Extracts JWT tokens from Authorization headers
 *    - Validates tokens and sets authentication context
 *    - Handles token blacklist checking
 *    - Provides comprehensive error handling and logging
 * 
 * 3. JwtAuthenticationEntryPoint - Handles authentication errors
 *    - Returns JSON error responses for unauthorized access
 *    - Provides consistent error format across the application
 * 
 * 4. TokenBlacklistService - Manages blacklisted JWT tokens
 *    - Maintains a list of invalidated tokens (e.g., after logout)
 *    - Automatically cleans up expired tokens to prevent memory leaks
 *    - Provides thread-safe token blacklist operations
 * 
 * 5. CustomUserDetailsService - Loads user details from database
 *    - Integrates with Spring Security authentication
 *    - Maps User entities to UserPrincipal objects
 *    - Supports user lookup by email and ID
 * 
 * 6. UserPrincipal - Spring Security UserDetails implementation
 *    - Wraps User entity for authentication purposes
 *    - Provides role-based authority mapping
 *    - Supports hierarchical role permissions (ADMIN > MANAGER > EMPLOYEE)
 * 
 * Security Features:
 * - Secure secret key generation with minimum 256-bit entropy
 * - Configurable token expiration times
 * - Token blacklisting for secure logout
 * - Comprehensive input validation and error handling
 * - Audit logging for security events
 * - Protection against common JWT vulnerabilities
 * 
 * Configuration:
 * JWT settings can be configured via application.properties:
 * - app.jwt.secret: JWT signing secret (auto-generated if not provided)
 * - app.jwt.expiration: Access token expiration time in milliseconds
 * - app.jwt.refresh-expiration: Refresh token expiration time in milliseconds
 * - app.jwt.issuer: Token issuer identifier
 * - app.jwt.session-timeout-minutes: Session timeout in minutes
 * 
 * @author Employee Portal Development Team
 * @version 1.0
 * @since 1.0
 */
package com.company.employeeportal.security;
