package com.company.employeeportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JWT Configuration properties.
 * Handles JWT token settings including secret key, expiration times, and security parameters.
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    
    private String secret;
    private long expiration = 3600000; // 1 hour in milliseconds
    private long refreshExpiration = 86400000; // 24 hours in milliseconds
    private String issuer = "employee-portal";
    private int sessionTimeoutMinutes = 30;

    @PostConstruct
    public void init() {
        // Generate a secure secret key if not provided
        if (secret == null || secret.trim().isEmpty() || "mySecretKey".equals(secret)) {
            generateSecureSecret();
        }
        
        // Validate minimum key length for HS256
        if (secret.getBytes().length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes long for HS256 algorithm");
        }
    }

    /**
     * Generate a cryptographically secure secret key
     */
    private void generateSecureSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] secretBytes = new byte[64]; // 512 bits
        secureRandom.nextBytes(secretBytes);
        this.secret = Base64.getEncoder().encodeToString(secretBytes);
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    /**
     * Get session timeout in milliseconds
     */
    public long getSessionTimeoutMillis() {
        return sessionTimeoutMinutes * 60L * 1000L;
    }
}
