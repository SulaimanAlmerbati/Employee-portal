package com.company.employeeportal.security;

import com.company.employeeportal.config.JwtConfig;
import com.company.employeeportal.dto.LoginRequest;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security tests specifically addressing task 3.5 requirements:
 * - Test JWT token generation and validation
 * - Verify role-based access control  
 * - Test authentication failure scenarios
 * 
 * Requirements: 1.2, 1.5
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ComprehensiveSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private User testEmployee;
    private User testManager;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        setupTestUsers();
        tokenBlacklistService.clearBlacklist();
    }

    @Test
    @DisplayName("JWT Token Generation and Validation - Complete Flow")
    void testJwtTokenGenerationAndValidation() throws Exception {
        // Test 1: Generate token through login endpoint
        LoginRequest loginRequest = new LoginRequest("employee@test.com", "password123");
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        // Extract tokens from response
        String responseBody = loginResult.getResponse().getContentAsString();
        assertTrue(responseBody.contains("accessToken"));
        assertTrue(responseBody.contains("refreshToken"));

        // Test 2: Direct token generation and validation
        UserPrincipal userPrincipal = UserPrincipal.create(testEmployee);
        String directToken = jwtUtil.generateToken(userPrincipal);
        
        // Validate token properties
        assertNotNull(directToken);
        assertTrue(directToken.split("\\.").length == 3); // JWT has 3 parts
        assertTrue(jwtUtil.validateToken(directToken, userPrincipal));
        assertEquals("employee@test.com", jwtUtil.extractUsername(directToken));
        
        // Test 3: Refresh token generation and validation
        String refreshToken = jwtUtil.generateRefreshToken(userPrincipal);
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        assertTrue(jwtUtil.validateToken(refreshToken, userPrincipal));
        
        // Test 4: Token refresh functionality
        String newAccessToken = jwtUtil.refreshAccessToken(refreshToken, userPrincipal);
        assertNotNull(newAccessToken);
        assertFalse(jwtUtil.isRefreshToken(newAccessToken));
        assertTrue(jwtUtil.validateToken(newAccessToken, userPrincipal));

        // Test 5: Invalid token rejection
        assertFalse(jwtUtil.validateToken("invalid.jwt.token", userPrincipal));
        assertFalse(jwtUtil.validateToken("", userPrincipal));
        assertFalse(jwtUtil.validateToken(null, userPrincipal));
    }

    @Test
    @DisplayName("Role-Based Access Control - All Roles and Endpoints")
    void testRoleBasedAccessControl() throws Exception {
        String employeeToken = generateTokenForUser(testEmployee);
        String managerToken = generateTokenForUser(testManager);
        String adminToken = generateTokenForUser(testAdmin);

        // Test Employee Access Rights
        // Employee can access their own profile
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        // Employee can access their leave requests
        mockMvc.perform(get("/api/leaves/my-requests")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        // Employee CANNOT access manager endpoints
        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Employee CANNOT access admin endpoints
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Test Manager Access Rights
        // Manager can access employee endpoints
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        // Manager can access manager endpoints
        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        // Manager CANNOT access admin-only endpoints
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/announcements")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        // Test Admin Access Rights
        // Admin can access all endpoints
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        // Test Role Hierarchy in UserPrincipal
        UserPrincipal adminPrincipal = UserPrincipal.create(testAdmin);
        UserPrincipal managerPrincipal = UserPrincipal.create(testManager);
        UserPrincipal employeePrincipal = UserPrincipal.create(testEmployee);

        // Verify role hierarchy
        assertTrue(adminPrincipal.hasRole("ADMIN"));
        assertTrue(adminPrincipal.hasRole("MANAGER"));
        assertTrue(adminPrincipal.hasRole("EMPLOYEE"));

        assertFalse(managerPrincipal.hasRole("ADMIN"));
        assertTrue(managerPrincipal.hasRole("MANAGER"));
        assertTrue(managerPrincipal.hasRole("EMPLOYEE"));

        assertFalse(employeePrincipal.hasRole("ADMIN"));
        assertFalse(employeePrincipal.hasRole("MANAGER"));
        assertTrue(employeePrincipal.hasRole("EMPLOYEE"));
    }

    @Test
    @DisplayName("Authentication Failure Scenarios - All Cases")
    void testAuthenticationFailureScenarios() throws Exception {
        // Test 1: Invalid credentials
        LoginRequest invalidCredentials = new LoginRequest("employee@test.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCredentials)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        // Test 2: Non-existent user
        LoginRequest nonExistentUser = new LoginRequest("nonexistent@test.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        // Test 3: Inactive user
        User inactiveUser = new User();
        inactiveUser.setName("Inactive User");
        inactiveUser.setEmail("inactive@test.com");
        inactiveUser.setPassword(passwordEncoder.encode("password123"));
        inactiveUser.setRole(Role.EMPLOYEE);
        inactiveUser.setActive(false);
        userRepository.save(inactiveUser);

        LoginRequest inactiveUserLogin = new LoginRequest("inactive@test.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inactiveUserLogin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        // Test 4: Missing authorization header
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        // Test 5: Malformed authorization header
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());

        // Test 6: Invalid JWT token
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());

        // Test 7: Expired token
        JwtConfig shortConfig = new JwtConfig();
        shortConfig.setSecret("testSecretKeyThatIsLongEnoughForHS256AlgorithmAndMeetsMinimumRequirements");
        shortConfig.setExpiration(1L); // 1 millisecond
        shortConfig.setIssuer("test-issuer");
        
        JwtUtil shortJwtUtil = new JwtUtil(shortConfig);
        UserPrincipal userPrincipal = UserPrincipal.create(testEmployee);
        String expiredToken = shortJwtUtil.generateToken(userPrincipal);
        
        Thread.sleep(10); // Wait for expiration
        
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());

        // Test 8: Blacklisted token
        String validToken = generateTokenForUser(testEmployee);
        
        // Token should work initially
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound()); // 404 = accessible but not implemented

        // Blacklist the token
        tokenBlacklistService.blacklistToken(validToken);

        // Token should now be rejected
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());

        // Test 9: Input validation failures
        LoginRequest emptyRequest = new LoginRequest("", "");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());

        // Test 10: User deleted after token generation
        String tokenForDeletion = generateTokenForUser(testEmployee);
        userRepository.delete(testEmployee);
        
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + tokenForDeletion))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security Configuration Validation")
    void testSecurityConfiguration() throws Exception {
        // Test CORS configuration
        mockMvc.perform(options("/api/users/profile")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        // Test security headers
        String token = generateTokenForUser(testManager);
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));

        // Test public endpoints accessibility
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isNotFound()); // File doesn't exist but endpoint is accessible

        // Test CSRF is disabled (stateless API)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized()); // Unauthorized due to invalid credentials, not CSRF
    }

    @Test
    @DisplayName("Token Blacklist Service Functionality")
    void testTokenBlacklistService() {
        UserPrincipal userPrincipal = UserPrincipal.create(testEmployee);
        String token1 = jwtUtil.generateToken(userPrincipal);
        String token2 = jwtUtil.generateToken(userPrincipal);

        // Initially no tokens are blacklisted
        assertEquals(0, tokenBlacklistService.getBlacklistSize());
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token1));
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token2));

        // Blacklist first token
        tokenBlacklistService.blacklistToken(token1);
        assertEquals(1, tokenBlacklistService.getBlacklistSize());
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token1));
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token2));

        // Blacklist second token
        tokenBlacklistService.blacklistToken(token2);
        assertEquals(2, tokenBlacklistService.getBlacklistSize());
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token1));
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token2));

        // Clear blacklist
        tokenBlacklistService.clearBlacklist();
        assertEquals(0, tokenBlacklistService.getBlacklistSize());
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token1));
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token2));

        // Test null/empty token handling
        tokenBlacklistService.blacklistToken(null);
        tokenBlacklistService.blacklistToken("");
        tokenBlacklistService.blacklistToken("   ");
        assertEquals(0, tokenBlacklistService.getBlacklistSize());
        
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
        assertFalse(tokenBlacklistService.isTokenBlacklisted(""));
    }

    private void setupTestUsers() {
        testEmployee = new User();
        testEmployee.setName("Test Employee");
        testEmployee.setEmail("employee@test.com");
        testEmployee.setPassword(passwordEncoder.encode("password123"));
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setDepartment("IT");
        testEmployee.setPosition("Developer");
        testEmployee.setActive(true);
        userRepository.save(testEmployee);

        testManager = new User();
        testManager.setName("Test Manager");
        testManager.setEmail("manager@test.com");
        testManager.setPassword(passwordEncoder.encode("password123"));
        testManager.setRole(Role.MANAGER);
        testManager.setDepartment("IT");
        testManager.setPosition("Team Lead");
        testManager.setActive(true);
        userRepository.save(testManager);

        testAdmin = new User();
        testAdmin.setName("Test Admin");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPassword(passwordEncoder.encode("password123"));
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setDepartment("HR");
        testAdmin.setPosition("Administrator");
        testAdmin.setActive(true);
        userRepository.save(testAdmin);
    }

    private String generateTokenForUser(User user) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        return jwtUtil.generateToken(userPrincipal);
    }
}