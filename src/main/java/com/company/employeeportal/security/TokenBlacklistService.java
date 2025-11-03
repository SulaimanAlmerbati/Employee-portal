package com.company.employeeportal.security;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to manage blacklisted JWT tokens.
 * Provides functionality to blacklist tokens and check if a token is blacklisted.
 * Automatically cleans up expired tokens to prevent memory leaks.
 */
@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JwtUtil jwtUtil;

    public TokenBlacklistService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        
        // Schedule cleanup of expired tokens every hour
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Add a token to the blacklist
     */
    public void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.add(token);
        }
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        return token != null && blacklistedTokens.contains(token);
    }

    /**
     * Remove expired tokens from blacklist to prevent memory leaks
     */
    private void cleanupExpiredTokens() {
        blacklistedTokens.removeIf(token -> {
            try {
                return jwtUtil.getRemainingExpirationTime(token) <= 0;
            } catch (Exception e) {
                // Remove invalid tokens as well
                return true;
            }
        });
    }

    /**
     * Get the current size of the blacklist (for monitoring purposes)
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    /**
     * Clear all blacklisted tokens (for testing purposes)
     */
    public void clearBlacklist() {
        blacklistedTokens.clear();
    }
}
