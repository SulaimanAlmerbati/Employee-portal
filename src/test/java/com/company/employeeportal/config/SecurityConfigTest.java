package com.company.employeeportal.config;

import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import com.company.employeeportal.security.JwtUtil;
import com.company.employeeportal.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Spring Security configuration.
 * Tests role-based access control, CORS, CSRF, and authentication requirements.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private User employeeUser;
    private User managerUser;
    private User adminUser;
    private String employeeToken;
    private String managerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        setupTestUsers();
        setupTokens();
        setupMockRepository();
    }

    @Test
    void testPublicEndpointsAccessible() throws Exception {
        // Test login endpoint is accessible without authentication
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized()); // Will be unauthorized due to invalid credentials, but accessible

        // Test health endpoint is accessible
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        // Test static resources are accessible
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isNotFound()); // File doesn't exist but endpoint is accessible
    }

    @Test
    void testEmployeeEndpointsAccessControl() throws Exception {
        // Employee can access their own profile
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet

        // Employee can access their leave requests
        mockMvc.perform(get("/api/leaves/my-requests")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet

        // Employee can access announcements
        mockMvc.perform(get("/api/announcements")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet
    }

    @Test
    void testManagerEndpointsAccessControl() throws Exception {
        // Manager can access pending leave requests
        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet

        // Employee cannot access manager endpoints
        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Admin can access manager endpoints
        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet
    }

    @Test
    void testAdminEndpointsAccessControl() throws Exception {
        // Admin can access user management
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet

        // Manager cannot access admin endpoints
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // Employee cannot access admin endpoints
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthorizedAccessDenied() throws Exception {
        // Access without token should be denied
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        // Access with invalid token should be denied
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCorsConfiguration() throws Exception {
        // Test CORS preflight request
        mockMvc.perform(options("/api/users/profile")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void testCsrfDisabled() throws Exception {
        // POST request without CSRF token should work (CSRF is disabled for stateless API)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized()); // Will be unauthorized due to invalid credentials, but CSRF is not blocking
    }

    @Test
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void testRoleHierarchy() throws Exception {
        // Admin should have access to all endpoints
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet

        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet

        // Manager should have access to employee and manager endpoints
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet

        mockMvc.perform(get("/api/leaves/pending")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound()); // Endpoint exists but controller not implemented yet
    }

    private void setupTestUsers() {
        employeeUser = createUser(1L, "employee@example.com", Role.EMPLOYEE);
        managerUser = createUser(2L, "manager@example.com", Role.MANAGER);
        adminUser = createUser(3L, "admin@example.com", Role.ADMIN);
    }

    private void setupTokens() {
        employeeToken = jwtUtil.generateToken(UserPrincipal.create(employeeUser));
        managerToken = jwtUtil.generateToken(UserPrincipal.create(managerUser));
        adminToken = jwtUtil.generateToken(UserPrincipal.create(adminUser));
    }

    private void setupMockRepository() {
        when(userRepository.findByEmailAndActive(eq("employee@example.com"), eq(true)))
                .thenReturn(Optional.of(employeeUser));
        when(userRepository.findByEmailAndActive(eq("manager@example.com"), eq(true)))
                .thenReturn(Optional.of(managerUser));
        when(userRepository.findByEmailAndActive(eq("admin@example.com"), eq(true)))
                .thenReturn(Optional.of(adminUser));
    }

    private User createUser(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setDepartment("IT");
        user.setPosition("Developer");
        user.setActive(true);
        return user;
    }
}
