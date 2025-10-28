package com.company.employeeportal.controller;

import com.company.employeeportal.dto.ChangePasswordRequest;
import com.company.employeeportal.dto.UserProfileResponse;
import com.company.employeeportal.dto.UserProfileUpdateRequest;
import com.company.employeeportal.dto.UserRequest;
import com.company.employeeportal.dto.UserResponse;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.security.UserPrincipal;
import com.company.employeeportal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for user management operations.
 * Handles user profile management and admin user operations.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current user's profile.
     * 
     * @param userPrincipal the authenticated user
     * @return user profile response
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        UserProfileResponse response = userService.getUserProfileResponse(userPrincipal.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user's profile.
     * 
     * @param updateRequest the updated user data
     * @param userPrincipal the authenticated user
     * @return updated user profile response
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest updateRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        UserProfileResponse response = userService.updateUserProfile(userPrincipal.getId(), updateRequest, currentUser);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Change current user's password.
     * 
     * @param changePasswordRequest the password change request
     * @param userPrincipal the authenticated user
     * @return success response
     */
    @PostMapping("/profile/change-password")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        userService.changePassword(userPrincipal.getId(), changePasswordRequest, currentUser);
        
        return ResponseEntity.ok("Password changed successfully");
    }

    /**
     * Get all users (admin only).
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @param sort sort field (default: name)
     * @param direction sort direction (default: asc)
     * @param userPrincipal the authenticated user
     * @return page of users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        Page<UserResponse> response = userService.getUsersWithPaginationResponse(pageable, currentUser);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Search users by name or email (admin only).
     * 
     * @param searchTerm the search term
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @param userPrincipal the authenticated user
     * @return page of matching users
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        
        Page<User> users = userService.searchUsers(searchTerm, pageable, currentUser);
        Page<UserResponse> response = users.map(this::convertToUserResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get users by role (admin only).
     * 
     * @param role the role to filter by
     * @param userPrincipal the authenticated user
     * @return list of users with the specified role
     */
    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersByRole(
            @PathVariable Role role,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        List<User> users = userService.getUsersByRole(role, currentUser);
        
        List<UserResponse> response = users.stream()
            .map(this::convertToUserResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID (admin only).
     * 
     * @param userId the user ID
     * @param userPrincipal the authenticated user
     * @return user details
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User user = userService.getUserProfile(userId);
        UserResponse response = convertToUserResponse(user);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create new user (admin only).
     * 
     * @param userRequest the new user data
     * @param userPrincipal the authenticated user
     * @return created user response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequest userRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        UserResponse response = userService.createUser(userRequest, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update user by ID (admin only).
     * 
     * @param userId the user ID to update
     * @param userRequest the updated user data
     * @param userPrincipal the authenticated user
     * @return updated user response
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest userRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        User updatedUserData = convertToUser(userRequest);
        
        User updatedUser = userService.updateUserByAdmin(userId, updatedUserData, currentUser);
        UserResponse response = convertToUserResponse(updatedUser);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate user (admin only).
     * 
     * @param userId the user ID to deactivate
     * @param userPrincipal the authenticated user
     * @return success response
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        userService.deactivateUser(userId, currentUser);
        
        return ResponseEntity.ok("User deactivated successfully");
    }

    /**
     * Reactivate user (admin only).
     * 
     * @param userId the user ID to reactivate
     * @param userPrincipal the authenticated user
     * @return success response
     */
    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reactivateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        userService.reactivateUser(userId, currentUser);
        
        return ResponseEntity.ok("User reactivated successfully");
    }

    /**
     * Reset user password (admin only).
     * 
     * @param userId the user ID
     * @param newPassword the new password
     * @param userPrincipal the authenticated user
     * @return success response
     */
    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetUserPassword(
            @PathVariable Long userId,
            @RequestParam String newPassword,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        User currentUser = userService.getUserProfile(userPrincipal.getId());
        userService.changePassword(userId, null, newPassword, currentUser);
        
        return ResponseEntity.ok("Password reset successfully");
    }

    /**
     * Convert User entity to UserProfileResponse DTO.
     */
    private UserProfileResponse convertToProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setDepartment(user.getDepartment());
        response.setPosition(user.getPosition());
        response.setContactInfo(user.getContactInfo());
        response.setJoinDate(user.getJoinDate());
        response.setActive(user.getActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    /**
     * Convert User entity to UserResponse DTO.
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setDepartment(user.getDepartment());
        response.setPosition(user.getPosition());
        response.setContactInfo(user.getContactInfo());
        response.setJoinDate(user.getJoinDate());
        response.setActive(user.getActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    /**
     * Convert UserRequest DTO to User entity.
     */
    private User convertToUser(UserRequest userRequest) {
        User user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setPassword(userRequest.getPassword());
        user.setRole(userRequest.getRole());
        user.setDepartment(userRequest.getDepartment());
        user.setPosition(userRequest.getPosition());
        user.setContactInfo(userRequest.getContactInfo());
        user.setJoinDate(userRequest.getJoinDate());
        return user;
    }
}