package com.company.employeeportal.security;

import com.company.employeeportal.config.JwtConfig;
import com.company.employeeportal.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil class.
 * Tests JWT token generation, validation, and claims extraction.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtConfig jwtConfig;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("testSecretKeyThatIsLongEnoughForHS256AlgorithmAndMeetsMinimumRequirements");
        jwtConfig.setExpiration(3600000L); // 1 hour
        jwtConfig.setRefreshExpiration(86400000L); // 24 hours
        jwtConfig.setIssuer("test-issuer");
        jwtConfig.setSessionTimeoutMinutes(30);
        
        jwtUtil = new JwtUtil(jwtConfig);
        
        // Create test user principal
        userDetails = UserPrincipal.create(createTestUser());
    }

    @Test
    void testGenerateToken() {
        String token = jwtUtil.generateToken(userDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testExtractUsername() {
        String token = jwtUtil.generateToken(userDetails);
        String extractedUsername = jwtUtil.extractUsername(token);
        
        assertEquals(userDetails.getUsername(), extractedUsername);
    }

    @Test
    void testValidateToken() {
        String token = jwtUtil.generateToken(userDetails);
        
        assertTrue(jwtUtil.validateToken(token, userDetails));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testGenerateRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        assertNotNull(refreshToken);
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        assertTrue(jwtUtil.validateToken(refreshToken));
    }

    @Test
    void testTokenWithExtraClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "EMPLOYEE");
        extraClaims.put("department", "IT");
        
        String token = jwtUtil.generateToken(userDetails, extraClaims);
        
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.jwt.token";
        
        assertFalse(jwtUtil.validateToken(invalidToken));
        assertFalse(jwtUtil.validateToken(invalidToken, userDetails));
    }

    @Test
    void testExpiredToken() {
        // Create config with very short expiration
        JwtConfig shortConfig = new JwtConfig();
        shortConfig.setSecret("testSecretKeyThatIsLongEnoughForHS256AlgorithmAndMeetsMinimumRequirements");
        shortConfig.setExpiration(1L); // 1 millisecond
        shortConfig.setIssuer("test-issuer");
        
        JwtUtil shortJwtUtil = new JwtUtil(shortConfig);
        String token = shortJwtUtil.generateToken(userDetails);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertFalse(shortJwtUtil.validateToken(token, userDetails));
    }

    @Test
    void testExtractIssuer() {
        String token = jwtUtil.generateToken(userDetails);
        String extractedIssuer = jwtUtil.extractIssuer(token);
        
        assertEquals("test-issuer", extractedIssuer);
    }

    @Test
    void testWillExpireWithin() {
        String token = jwtUtil.generateToken(userDetails);
        
        // Token should not expire within 1 minute (it has 1 hour expiration)
        assertFalse(jwtUtil.willExpireWithin(token, 1));
        
        // Token should expire within 2 hours
        assertTrue(jwtUtil.willExpireWithin(token, 120));
    }

    @Test
    void testRefreshAccessToken() {
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        String newAccessToken = jwtUtil.refreshAccessToken(refreshToken, userDetails);
        
        assertNotNull(newAccessToken);
        assertFalse(jwtUtil.isRefreshToken(newAccessToken));
        assertTrue(jwtUtil.validateToken(newAccessToken, userDetails));
    }

    @Test
    void testRefreshAccessTokenWithInvalidRefreshToken() {
        String accessToken = jwtUtil.generateToken(userDetails);
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.refreshAccessToken(accessToken, userDetails);
        });
    }

    @Test
    void testIsValidTokenStructure() {
        String validToken = jwtUtil.generateToken(userDetails);
        String invalidToken = "invalid.token.structure";
        
        assertTrue(jwtUtil.isValidTokenStructure(validToken));
        assertFalse(jwtUtil.isValidTokenStructure(invalidToken));
    }

    @Test
    void testTokenSecurityFeatures() {
        String token = jwtUtil.generateToken(userDetails);
        
        // Test token expiration time
        assertTrue(jwtUtil.getRemainingExpirationTime(token) > 0);
        assertTrue(jwtUtil.getRemainingExpirationTime(token) <= jwtConfig.getExpiration());
        
        // Test token issued before functionality
        assertFalse(jwtUtil.isTokenIssuedBefore(token, new java.util.Date(System.currentTimeMillis() - 1000)));
        assertTrue(jwtUtil.isTokenIssuedBefore(token, new java.util.Date(System.currentTimeMillis() + 1000)));
        
        // Test role and user ID extraction (should return null for basic token)
        assertNull(jwtUtil.extractRole(token));
        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void testTokenWithCustomClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "EMPLOYEE");
        claims.put("userId", 123L);
        
        String token = jwtUtil.generateToken(userDetails, claims);
        
        assertEquals("EMPLOYEE", jwtUtil.extractRole(token));
        assertEquals(Long.valueOf(123L), jwtUtil.extractUserId(token));
    }

    @Test
    void testInvalidTokenHandling() {
        // Test various invalid token scenarios
        assertEquals(0, jwtUtil.getRemainingExpirationTime("invalid.token"));
        assertTrue(jwtUtil.isTokenIssuedBefore("invalid.token", new java.util.Date()));
        assertNull(jwtUtil.extractRole("invalid.token"));
        assertNull(jwtUtil.extractUserId("invalid.token"));
        
        // Test null token handling
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.extractUsername(null));
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
