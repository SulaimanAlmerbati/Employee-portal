package com.company.employeeportal.security;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for all security-related tests.
 * This suite covers all requirements for task 3.5:
 * - JWT token generation and validation
 * - Role-based access control
 * - Authentication failure scenarios
 * 
 * Requirements: 1.2, 1.5
 */
@Suite
@SelectClasses({
    // JWT Token Generation and Validation Tests
    JwtUtilTest.class,
    
    // Authentication Integration Tests
    JwtAuthenticationIntegrationTest.class,
    
    // User Details Service Tests
    CustomUserDetailsServiceTest.class,
    
    // Token Blacklist Service Tests
    TokenBlacklistServiceTest.class,
    
    // Authentication Controller Tests
    com.company.employeeportal.controller.AuthControllerTest.class,
    
    // Security Configuration Tests
    com.company.employeeportal.config.SecurityConfigTest.class,
    
    // Comprehensive Security Integration Tests
    SecurityIntegrationTest.class,
    ComprehensiveSecurityTest.class
})
public class SecurityTestSuite {
    // This class serves as a test suite aggregator
    // All security tests are included and can be run together
}