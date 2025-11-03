package com.company.employeeportal.security;

import com.company.employeeportal.config.JwtConfig;
import com.company.employeeportal.dto.LoginRequest;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security integration tests covering:
 * - JWT token generation and validation
 * - Role-based access control
 * - Authentication failure scenarios
 * 
 * Requirements: 1.2, 1.5
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

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

    @Autowired
    private JwtConfig jwtConfig;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private User employeeUser;
    private User managerUser;
    private User adminUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        setupTestUsers();
        tokenBlacklistService.clearBlacklist();
    }

    @Nested
    @DisplayName("JWT Token Generation and Validation Tests")
    class JwtTokenTests {

        @Test
        @DisplayName("Should generate valid JWT token on successful login")
        void testJwtTokenGeneration() throws Exception {
            LoginRequest loginRequest = new LoginRequest("employee@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.refreshToken").isString())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.email").value("employee@example.com"))
                    .andExpect(jsonPath("$.user.role").value("EMPLOYEE"));
        }

        @Test
        @DisplayName("Should validate JWT token structure and claims")
        void testJwtTokenValidation() {
            UserPrincipal userPrincipal = UserPrincipal.create(employeeUser);
            String token = jwtUtil.generateToken(userPrincipal);

            // Validate token structure
            assertTrue(jwtUtil.isValidTokenStructure(token));
            assertTrue(jwtUtil.validateToken(token, userPrincipal));
            
            // Validate token claims
            assertEquals("employee@example.com", jwtUtil.extractUsername(token));
            assertEquals(jwtConfig.getIssuer(), jwtUtil.extractIssuer(token));
            assertNotNull(jwtUtil.extractExpiration(token));
            assertNotNull(jwtUtil.extractIssuedAt(token));
        }

        @Test
        @DisplayName("Should generate and validate refresh tokens")
        void testRefreshTokenGeneration() {
            UserPrincipal userPrincipal = UserPrincipal.create(employeeUser);
            String refreshToken = jwtUtil.generateRefreshToken(userPrincipal);

            assertTrue(jwtUtil.isRefreshToken(refreshToken));
            assertTrue(jwtUtil.validateToken(refreshToken, userPrincipal));
            
            // Should be able to generate new access token from refresh token
            String newAccessToken = jwtUtil.refreshAccessToken(refreshToken, userPrincipal);
            assertNotNull(newAccessToken);
            assertFalse(jwtUtil.isRefreshToken(newAccessToken));
            assertTrue(jwtUtil.validateToken(newAccessToken, userPrincipal));
        }

        @Test
        @DisplayName("Should reject expired JWT tokens")
        void testExpiredTokenRejection() {
            // Create config with very short expiration
            JwtConfig shortConfig = new JwtConfig();
            shortConfig.setSecret("testSecretKeyThatIsLongEnoughForHS256AlgorithmAndMeetsMinimumRequirements");
            shortConfig.setExpiration(1L); // 1 millisecond
            shortConfig.setIssuer("test-issuer");
            
            JwtUtil shortJwtUtil = new JwtUtil(shortConfig);
            UserPrincipal userPrincipal = UserPrincipal.create(employeeUser);
            String expiredToken = shortJwtUtil.generateToken(userPrincipal);
            
            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            assertFalse(shortJwtUtil.validateToken(expiredToken, userPrincipal));
        }

        @Test
        @DisplayName("Should reject malformed JWT tokens")
        void testMalformedTokenRejection() {
            UserPrincipal userPrincipal = UserPrincipal.create(employeeUser);
            
            // Test various malformed tokens
            assertFalse(jwtUtil.validateToken("invalid.token.here", userPrincipal));
            assertFalse(jwtUtil.validateToken("", userPrincipal));
            assertFalse(jwtUtil.validateToken(null, userPrincipal));
            assertFalse(jwtUtil.validateToken("not-a-jwt-token", userPrincipal));
            assertFalse(jwtUtil.isValidTokenStructure("malformed"));
        }

        @Test
        @DisplayName("Should handle token blacklisting")
        void testTokenBlacklisting() {
            UserPrincipal userPrincipal = UserPrincipal.create(employeeUser);
            String token = jwtUtil.generateToken(userPrincipal);
            
            // Token should be valid initially
            assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
            
            // Blacklist the token
            tokenBlacklistService.blacklistToken(token);
            
            // Token should now be blacklisted
            assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
        }
    }

    @Nested
    @DisplayName("Role-Based Access Control Tests")
    class RoleBasedAccessControlTests {

        @Test
        @DisplayName("Employee should access only employee endpoints")
        void testEmployeeAccessControl() throws Exception {
            String employeeToken = generateTokenForUser(employeeUser);

            // Employee can access employee endpoints
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            mockMvc.perform(get("/api/leaves/my-requests")
                    .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            // Employee cannot access manager endpoints
            mockMvc.perform(get("/api/leaves/pending")
                    .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());

            // Employee cannot access admin endpoints
            mockMvc.perform(get("/api/users")
                    .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/admin/dashboard")
                    .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Manager should access employee and manager endpoints")
        void testManagerAccessControl() throws Exception {
            String managerToken = generateTokenForUser(managerUser);

            // Manager can access employee endpoints
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            // Manager can access manager endpoints
            mockMvc.perform(get("/api/leaves/pending")
                    .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            // Manager cannot access admin-only endpoints
            mockMvc.perform(get("/api/users")
                    .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/announcements")
                    .header("Authorization", "Bearer " + managerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin should access all endpoints")
        void testAdminAccessControl() throws Exception {
            String adminToken = generateTokenForUser(adminUser);

            // Admin can access employee endpoints
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            // Admin can access manager endpoints
            mockMvc.perform(get("/api/leaves/pending")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            // Admin can access admin endpoints
            mockMvc.perform(get("/api/users")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            mockMvc.perform(get("/api/admin/dashboard")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet
        }

        @Test
        @DisplayName("Should enforce method-level security annotations")
        @WithMockUser(roles = "EMPLOYEE")
        void testMethodLevelSecurity() throws Exception {
            // This test verifies that @PreAuthorize and similar annotations work
            // The actual implementation will be tested when controllers are implemented
            
            // For now, test that the security configuration is properly set up
            mockMvc.perform(get("/api/admin/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should handle role hierarchy correctly")
        void testRoleHierarchy() {
            // Test that role hierarchy is working (ADMIN > MANAGER > EMPLOYEE)
            UserPrincipal adminPrincipal = UserPrincipal.create(adminUser);
            UserPrincipal managerPrincipal = UserPrincipal.create(managerUser);
            UserPrincipal employeePrincipal = UserPrincipal.create(employeeUser);

            // Admin should have all roles
            assertTrue(adminPrincipal.hasRole("ADMIN"));
            assertTrue(adminPrincipal.hasRole("MANAGER"));
            assertTrue(adminPrincipal.hasRole("EMPLOYEE"));

            // Manager should have manager and employee roles
            assertFalse(managerPrincipal.hasRole("ADMIN"));
            assertTrue(managerPrincipal.hasRole("MANAGER"));
            assertTrue(managerPrincipal.hasRole("EMPLOYEE"));

            // Employee should only have employee role
            assertFalse(employeePrincipal.hasRole("ADMIN"));
            assertFalse(employeePrincipal.hasRole("MANAGER"));
            assertTrue(employeePrincipal.hasRole("EMPLOYEE"));
        }
    }

    @Nested
    @DisplayName("Authentication Failure Scenarios")
    class AuthenticationFailureTests {

        @Test
        @DisplayName("Should reject login with invalid credentials")
        void testInvalidCredentials() throws Exception {
            LoginRequest loginRequest = new LoginRequest("employee@example.com", "wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should reject login for non-existent user")
        void testNonExistentUser() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should reject login for inactive user")
        void testInactiveUser() throws Exception {
            LoginRequest loginRequest = new LoginRequest("inactive@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should reject requests with missing authorization header")
        void testMissingAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/users/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject requests with malformed authorization header")
        void testMalformedAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "InvalidFormat token"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject requests with blacklisted tokens")
        void testBlacklistedToken() throws Exception {
            String token = generateTokenForUser(employeeUser);
            
            // Token should work initially
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound()); // 404 because controller not implemented yet

            // Blacklist the token
            tokenBlacklistService.blacklistToken(token);

            // Token should now be rejected
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle JWT parsing exceptions gracefully")
        void testJwtParsingExceptions() throws Exception {
            // Test various malformed JWT tokens
            String[] malformedTokens = {
                "invalid.jwt.token",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature",
                "not-a-jwt-at-all",
                "",
                "Bearer-without-space"
            };

            for (String malformedToken : malformedTokens) {
                mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + malformedToken))
                        .andExpect(status().isUnauthorized());
            }
        }

        @Test
        @DisplayName("Should handle user details loading failures")
        void testUserDetailsLoadingFailure() throws Exception {
            // Create a token for a user that will be deleted
            String token = generateTokenForUser(employeeUser);
            
            // Delete the user from repository
            userRepository.delete(employeeUser);

            // Token should now be rejected because user cannot be loaded
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should validate input parameters for login")
        void testLoginInputValidation() throws Exception {
            // Test empty email and password
            LoginRequest emptyRequest = new LoginRequest("", "");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emptyRequest)))
                    .andExpect(status().isBadRequest());

            // Test null values
            LoginRequest nullRequest = new LoginRequest(null, null);
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nullRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle session timeout scenarios")
        void testSessionTimeout() throws Exception {
            // Create a token that will expire soon
            JwtConfig shortConfig = new JwtConfig();
            shortConfig.setSecret("testSecretKeyThatIsLongEnoughForHS256AlgorithmAndMeetsMinimumRequirements");
            shortConfig.setExpiration(100L); // 100 milliseconds
            shortConfig.setIssuer("test-issuer");
            
            JwtUtil shortJwtUtil = new JwtUtil(shortConfig);
            UserPrincipal userPrincipal = UserPrincipal.create(employeeUser);
            String shortLivedToken = shortJwtUtil.generateToken(userPrincipal);

            // Wait for token to expire
            Thread.sleep(200);

            // Request should be rejected due to expired token
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + shortLivedToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CORS and Security Headers Tests")
    class SecurityHeadersTests {

        @Test
        @DisplayName("Should handle CORS preflight requests")
        void testCorsPreflightRequest() throws Exception {
            mockMvc.perform(options("/api/users/profile")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                    .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }

        @Test
        @DisplayName("Should set security headers")
        void testSecurityHeaders() throws Exception {
            String token = generateTokenForUser(employeeUser);

            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
        }

        @Test
        @DisplayName("Should allow access to public endpoints without authentication")
        void testPublicEndpointsAccess() throws Exception {
            // Health endpoint should be accessible
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());

            // Static resources should be accessible
            mockMvc.perform(get("/css/style.css"))
                    .andExpect(status().isNotFound()); // File doesn't exist but endpoint is accessible

            // Login page should be accessible
            mockMvc.perform(get("/login"))
                    .andExpect(status().isNotFound()); // Template not implemented yet but endpoint is accessible
        }
    }

    private void setupTestUsers() {
        // Employee user
        employeeUser = new User();
        employeeUser.setName("Employee User");
        employeeUser.setEmail("employee@example.com");
        employeeUser.setPassword(passwordEncoder.encode("password123"));
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setDepartment("IT");
        employeeUser.setPosition("Developer");
        employeeUser.setActive(true);
        userRepository.save(employeeUser);

        // Manager user
        managerUser = new User();
        managerUser.setName("Manager User");
        managerUser.setEmail("manager@example.com");
        managerUser.setPassword(passwordEncoder.encode("password123"));
        managerUser.setRole(Role.MANAGER);
        managerUser.setDepartment("IT");
        managerUser.setPosition("Team Lead");
        managerUser.setActive(true);
        userRepository.save(managerUser);

        // Admin user
        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setDepartment("HR");
        adminUser.setPosition("Administrator");
        adminUser.setActive(true);
        userRepository.save(adminUser);

        // Inactive user
        inactiveUser = new User();
        inactiveUser.setName("Inactive User");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPassword(passwordEncoder.encode("password123"));
        inactiveUser.setRole(Role.EMPLOYEE);
        inactiveUser.setDepartment("IT");
        inactiveUser.setPosition("Developer");
        inactiveUser.setActive(false);
        userRepository.save(inactiveUser);
    }

    private String generateTokenForUser(User user) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        return jwtUtil.generateToken(userPrincipal);
    }
}
