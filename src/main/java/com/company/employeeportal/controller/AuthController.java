package com.company.employeeportal.controller;

import com.company.employeeportal.dto.AuthResponse;
import com.company.employeeportal.dto.LoginRequest;
import com.company.employeeportal.dto.LogoutRequest;
import com.company.employeeportal.dto.RefreshTokenRequest;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import com.company.employeeportal.security.JwtUtil;
import com.company.employeeportal.security.TokenBlacklistService;
import com.company.employeeportal.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for authentication operations.
 * Handles login, logout, and token refresh functionality.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         JwtUtil jwtUtil,
                         UserDetailsService userDetailsService,
                         TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Authenticate user and generate JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user credentials
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            // Generate tokens
            String accessToken = jwtUtil.generateToken(userPrincipal);
            String refreshToken = jwtUtil.generateRefreshToken(userPrincipal);

            // Get user details for response
            User user = userRepository.findByEmail(userPrincipal.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment(),
                user.getPosition()
            );

            AuthResponse authResponse = new AuthResponse(
                accessToken,
                refreshToken,
                jwtUtil.getExpirationTime(),
                userInfo
            );

            return ResponseEntity.ok(authResponse);

        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            error.put("message", "Email or password is incorrect");
            return ResponseEntity.badRequest().body(error);
        } catch (DisabledException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Account disabled");
            error.put("message", "Your account has been disabled");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");
            error.put("message", "An error occurred during authentication");
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            // Check if refresh token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Token has been invalidated");
                error.put("message", "Please login again");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate refresh token
            if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid refresh token");
                error.put("message", "Refresh token is expired or invalid");
                return ResponseEntity.badRequest().body(error);
            }

            // Extract username and generate new access token
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Validate that the user still exists and is active
            if (!((UserPrincipal) userDetails).isEnabled()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Account disabled");
                error.put("message", "Your account has been disabled");
                return ResponseEntity.badRequest().body(error);
            }

            String newAccessToken = jwtUtil.generateToken(userDetails);

            // Get user details for response
            UserPrincipal userPrincipal = (UserPrincipal) userDetails;
            User user = userRepository.findByEmail(userPrincipal.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment(),
                user.getPosition()
            );

            AuthResponse authResponse = new AuthResponse(
                newAccessToken,
                refreshToken, // Keep the same refresh token
                jwtUtil.getExpirationTime(),
                userInfo
            );

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token refresh failed");
            error.put("message", "Unable to refresh token. Please login again.");
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Logout endpoint with token invalidation
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, @RequestBody(required = false) LogoutRequest logoutRequest) {
        try {
            // Extract access token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);
                
                // Add access token to blacklist to prevent further use
                tokenBlacklistService.blacklistToken(accessToken);
            }
            
            // If refresh token is provided in request body, blacklist it too
            if (logoutRequest != null && logoutRequest.getRefreshToken() != null) {
                tokenBlacklistService.blacklistToken(logoutRequest.getRefreshToken());
            }
            
            // Clear security context
            SecurityContextHolder.clearContext();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Even if token blacklisting fails, we should still clear the context
            SecurityContextHolder.clearContext();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findByEmail(userPrincipal.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment(),
                user.getPosition()
            );

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get user information");
            return ResponseEntity.badRequest().body(error);
        }
    }
}