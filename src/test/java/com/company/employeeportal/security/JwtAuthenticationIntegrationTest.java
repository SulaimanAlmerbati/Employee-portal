package com.company.employeeportal.security;

import com.company.employeeportal.config.JwtConfig;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import jakarta.servlet.FilterChain;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for JWT authentication flow.
 * Tests the complete authentication process including filter, token validation, and security context.
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtAuthenticationIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenBlacklistService.clearBlacklist();
        
        testUser = createTestUser();
        userDetails = UserPrincipal.create(testUser);
        
        // Mock user repository
        when(userRepository.findByEmailAndActive(eq("test@example.com"), eq(true)))
                .thenReturn(Optional.of(testUser));
    }

    @Test
    void testSuccessfulAuthentication() throws Exception {
        String token = jwtUtil.generateToken(userDetails);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAuthenticationWithInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAuthenticationWithBlacklistedToken() throws Exception {
        String token = jwtUtil.generateToken(userDetails);
        tokenBlacklistService.blacklistToken(token);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAuthenticationWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAuthenticationWithInactiveUser() throws Exception {
        // Mock inactive user
        User inactiveUser = createTestUser();
        inactiveUser.setActive(false);
        
        when(userRepository.findByEmailAndActive(eq("test@example.com"), eq(true)))
                .thenReturn(Optional.empty());
        
        String token = jwtUtil.generateToken(userDetails);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testRefreshTokenFlow() {
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        String newAccessToken = jwtUtil.refreshAccessToken(refreshToken, userDetails);
        
        assertNotNull(newAccessToken);
        assertTrue(jwtUtil.validateToken(newAccessToken, userDetails));
        assertFalse(jwtUtil.isRefreshToken(newAccessToken));
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(Role.EMPLOYEE);
        user.setDepartment("IT");
        user.setPosition("Developer");
        user.setActive(true);
        return user;
    }
}
