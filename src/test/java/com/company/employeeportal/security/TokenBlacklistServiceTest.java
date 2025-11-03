package com.company.employeeportal.security;

import com.company.employeeportal.config.JwtConfig;
import com.company.employeeportal.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenBlacklistService.
 * Tests token blacklisting functionality and cleanup operations.
 */
class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;
    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("testSecretKeyThatIsLongEnoughForHS256AlgorithmAndMeetsMinimumRequirements");
        jwtConfig.setExpiration(3600000L); // 1 hour
        jwtConfig.setRefreshExpiration(86400000L); // 24 hours
        jwtConfig.setIssuer("test-issuer");
        
        jwtUtil = new JwtUtil(jwtConfig);
        tokenBlacklistService = new TokenBlacklistService(jwtUtil);
        
        // Create test user principal
        userDetails = UserPrincipal.create(createTestUser());
    }

    @Test
    void testBlacklistToken() {
        String token = jwtUtil.generateToken(userDetails);
        
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
        
        tokenBlacklistService.blacklistToken(token);
        
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void testBlacklistNullToken() {
        tokenBlacklistService.blacklistToken(null);
        tokenBlacklistService.blacklistToken("");
        tokenBlacklistService.blacklistToken("   ");
        
        assertEquals(0, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testIsTokenBlacklistedWithNullToken() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
    }

    @Test
    void testGetBlacklistSize() {
        assertEquals(0, tokenBlacklistService.getBlacklistSize());
        
        String token1 = jwtUtil.generateToken(userDetails);
        String token2 = jwtUtil.generateRefreshToken(userDetails);
        
        tokenBlacklistService.blacklistToken(token1);
        assertEquals(1, tokenBlacklistService.getBlacklistSize());
        
        tokenBlacklistService.blacklistToken(token2);
        assertEquals(2, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testClearBlacklist() {
        String token = jwtUtil.generateToken(userDetails);
        tokenBlacklistService.blacklistToken(token);
        
        assertEquals(1, tokenBlacklistService.getBlacklistSize());
        
        tokenBlacklistService.clearBlacklist();
        
        assertEquals(0, tokenBlacklistService.getBlacklistSize());
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
    }

    private com.company.employeeportal.model.User createTestUser() {
        com.company.employeeportal.model.User user = new com.company.employeeportal.model.User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(Role.EMPLOYEE);
        user.setActive(true);
        return user;
    }
}
