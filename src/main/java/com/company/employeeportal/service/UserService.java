package com.company.employeeportal.service;

import com.company.employeeportal.dto.*;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import com.company.employeeportal.exception.UserNotFoundException;
import com.company.employeeportal.exception.EmailAlreadyExistsException;
import com.company.employeeportal.exception.UnauthorizedAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for user management operations.
 * Handles user profile management, creation, updates, and business logic.
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieve user profile by ID.
     * 
     * @param userId the user ID
     * @return the user profile
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .filter(User::getActive)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Retrieve user profile by email.
     * 
     * @param email the user email
     * @return the user profile
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndActive(email, true)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    /**
     * Get user profile as DTO.
     * 
     * @param userId the user ID
     * @return the user profile response
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileResponse(Long userId) {
        User user = getUserProfile(userId);
        return convertToUserProfileResponse(user);
    }

    /**
     * Update user profile information using UserProfileUpdateRequest DTO.
     * Only allows updating editable fields for regular users.
     * 
     * @param userId the user ID
     * @param updateRequest the updated user data
     * @param currentUser the current authenticated user
     * @return the updated user profile response
     * @throws UserNotFoundException if user not found
     * @throws UnauthorizedAccessException if user tries to update restricted fields
     */
    public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest updateRequest, User currentUser) {
        User existingUser = getUserProfile(userId);
        
        // Check if user is updating their own profile or if admin is updating
        if (!existingUser.getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("You can only update your own profile");
        }

        // Set userId for validation
        updateRequest.setUserId(userId);

        // Log profile modification attempt for audit purposes (Requirement 2.5)
        logger.info("User profile update attempt - User ID: {}, Modified by: {}, Admin: {}", 
                   userId, currentUser.getId(), currentUser.isAdmin());

        // Validate and sanitize input
        validateUserProfileUpdateInput(updateRequest);
        
        // Update editable fields
        if (StringUtils.hasText(updateRequest.getName())) {
            existingUser.setName(sanitizeInput(updateRequest.getName().trim()));
        }
        
        if (StringUtils.hasText(updateRequest.getContactInfo())) {
            existingUser.setContactInfo(sanitizeInput(updateRequest.getContactInfo().trim()));
        }

        // Admin can update additional fields
        if (currentUser.isAdmin()) {
            if (StringUtils.hasText(updateRequest.getDepartment())) {
                existingUser.setDepartment(sanitizeInput(updateRequest.getDepartment().trim()));
            }
            
            if (StringUtils.hasText(updateRequest.getPosition())) {
                existingUser.setPosition(sanitizeInput(updateRequest.getPosition().trim()));
            }
            
            if (updateRequest.getRole() != null) {
                existingUser.setRole(updateRequest.getRole());
            }
            
            if (updateRequest.getJoinDate() != null) {
                existingUser.setJoinDate(updateRequest.getJoinDate());
            }
        }

        User savedUser = userRepository.save(existingUser);
        logger.info("User profile updated successfully - User ID: {}", userId);
        
        return convertToUserProfileResponse(savedUser);
    }

    /**
     * Update user profile information using DTO (legacy method for backward compatibility).
     * Only allows updating editable fields for regular users.
     * 
     * @param userId the user ID
     * @param userRequest the updated user data
     * @param currentUser the current authenticated user
     * @return the updated user profile response
     * @throws UserNotFoundException if user not found
     * @throws UnauthorizedAccessException if user tries to update restricted fields
     */
    public UserProfileResponse updateUserProfile(Long userId, UserRequest userRequest, User currentUser) {
        User existingUser = getUserProfile(userId);
        
        // Check if user is updating their own profile or if admin is updating
        if (!existingUser.getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("You can only update your own profile");
        }

        // Log profile modification attempt for audit purposes (Requirement 2.5)
        logger.info("User profile update attempt - User ID: {}, Modified by: {}, Admin: {}", 
                   userId, currentUser.getId(), currentUser.isAdmin());

        // Validate and sanitize input
        validateUserRequestInput(userRequest);
        
        // Update editable fields
        if (StringUtils.hasText(userRequest.getName())) {
            existingUser.setName(sanitizeInput(userRequest.getName().trim()));
        }
        
        if (StringUtils.hasText(userRequest.getContactInfo())) {
            existingUser.setContactInfo(sanitizeInput(userRequest.getContactInfo().trim()));
        }

        // Admin can update additional fields
        if (currentUser.isAdmin()) {
            if (StringUtils.hasText(userRequest.getDepartment())) {
                existingUser.setDepartment(sanitizeInput(userRequest.getDepartment().trim()));
            }
            
            if (StringUtils.hasText(userRequest.getPosition())) {
                existingUser.setPosition(sanitizeInput(userRequest.getPosition().trim()));
            }
            
            if (userRequest.getRole() != null) {
                existingUser.setRole(userRequest.getRole());
            }
            
            if (userRequest.getJoinDate() != null) {
                existingUser.setJoinDate(userRequest.getJoinDate());
            }
        }

        User savedUser = userRepository.save(existingUser);
        logger.info("User profile updated successfully - User ID: {}", userId);
        
        return convertToUserProfileResponse(savedUser);
    }

    /**
     * Update user profile information (legacy method for backward compatibility).
     * Only allows updating editable fields for regular users.
     * 
     * @param userId the user ID
     * @param updatedUser the updated user data
     * @param currentUser the current authenticated user
     * @return the updated user
     * @throws UserNotFoundException if user not found
     * @throws UnauthorizedAccessException if user tries to update restricted fields
     */
    public User updateUserProfile(Long userId, User updatedUser, User currentUser) {
        User existingUser = getUserProfile(userId);
        
        // Check if user is updating their own profile or if admin is updating
        if (!existingUser.getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("You can only update your own profile");
        }

        // Log profile modification attempt for audit purposes (Requirement 2.5)
        logger.info("User profile update attempt - User ID: {}, Modified by: {}, Admin: {}", 
                   userId, currentUser.getId(), currentUser.isAdmin());

        // Validate and sanitize input
        validateUserInput(updatedUser);
        
        // Update editable fields
        if (StringUtils.hasText(updatedUser.getName())) {
            existingUser.setName(sanitizeInput(updatedUser.getName().trim()));
        }
        
        if (StringUtils.hasText(updatedUser.getContactInfo())) {
            existingUser.setContactInfo(sanitizeInput(updatedUser.getContactInfo().trim()));
        }

        // Admin can update additional fields
        if (currentUser.isAdmin()) {
            if (StringUtils.hasText(updatedUser.getDepartment())) {
                existingUser.setDepartment(sanitizeInput(updatedUser.getDepartment().trim()));
            }
            
            if (StringUtils.hasText(updatedUser.getPosition())) {
                existingUser.setPosition(sanitizeInput(updatedUser.getPosition().trim()));
            }
            
            if (updatedUser.getRole() != null) {
                existingUser.setRole(updatedUser.getRole());
            }
            
            if (updatedUser.getJoinDate() != null) {
                existingUser.setJoinDate(updatedUser.getJoinDate());
            }
        }

        User savedUser = userRepository.save(existingUser);
        logger.info("User profile updated successfully - User ID: {}", userId);
        
        return savedUser;
    }

    /**
     * Create a new user using DTO (admin only).
     * 
     * @param userRequest the new user data
     * @param currentUser the current authenticated user
     * @return the created user response
     * @throws UnauthorizedAccessException if not admin
     * @throws EmailAlreadyExistsException if email already exists
     */
    public UserResponse createUser(UserRequest userRequest, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can create users");
        }

        logger.info("User creation attempt by admin - Admin ID: {}, Email: {}", 
                   currentUser.getId(), userRequest.getEmail());

        // Validate input
        validateUserRequestInput(userRequest);
        validateNewUserRequestData(userRequest);

        // Check if email already exists
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + userRequest.getEmail());
        }

        // Create new user entity
        User newUser = new User();
        
        // Sanitize and set input
        newUser.setName(sanitizeInput(userRequest.getName().trim()));
        newUser.setEmail(userRequest.getEmail().toLowerCase().trim());
        
        if (StringUtils.hasText(userRequest.getDepartment())) {
            newUser.setDepartment(sanitizeInput(userRequest.getDepartment().trim()));
        }
        
        if (StringUtils.hasText(userRequest.getPosition())) {
            newUser.setPosition(sanitizeInput(userRequest.getPosition().trim()));
        }
        
        if (StringUtils.hasText(userRequest.getContactInfo())) {
            newUser.setContactInfo(sanitizeInput(userRequest.getContactInfo().trim()));
        }

        // Encrypt password
        newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        
        // Set default values
        newUser.setActive(true);
        if (userRequest.getJoinDate() != null) {
            newUser.setJoinDate(userRequest.getJoinDate());
        } else {
            newUser.setJoinDate(LocalDate.now());
        }
        
        if (userRequest.getRole() != null) {
            newUser.setRole(userRequest.getRole());
        } else {
            newUser.setRole(Role.EMPLOYEE);
        }

        User savedUser = userRepository.save(newUser);
        logger.info("User created successfully - User ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
        
        return convertToUserResponse(savedUser);
    }

    /**
     * Create a new user (admin only) - legacy method for backward compatibility.
     * 
     * @param newUser the new user data
     * @param currentUser the current authenticated user
     * @return the created user
     * @throws UnauthorizedAccessException if not admin
     * @throws EmailAlreadyExistsException if email already exists
     */
    public User createUser(User newUser, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can create users");
        }

        logger.info("User creation attempt by admin - Admin ID: {}, Email: {}", 
                   currentUser.getId(), newUser.getEmail());

        // Validate input
        validateUserInput(newUser);
        validateNewUserData(newUser);

        // Check if email already exists
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + newUser.getEmail());
        }

        // Sanitize input
        newUser.setName(sanitizeInput(newUser.getName().trim()));
        newUser.setEmail(newUser.getEmail().toLowerCase().trim());
        
        if (StringUtils.hasText(newUser.getDepartment())) {
            newUser.setDepartment(sanitizeInput(newUser.getDepartment().trim()));
        }
        
        if (StringUtils.hasText(newUser.getPosition())) {
            newUser.setPosition(sanitizeInput(newUser.getPosition().trim()));
        }
        
        if (StringUtils.hasText(newUser.getContactInfo())) {
            newUser.setContactInfo(sanitizeInput(newUser.getContactInfo().trim()));
        }

        // Encrypt password
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        
        // Set default values
        newUser.setActive(true);
        if (newUser.getJoinDate() == null) {
            newUser.setJoinDate(LocalDate.now());
        }
        if (newUser.getRole() == null) {
            newUser.setRole(Role.EMPLOYEE);
        }

        User savedUser = userRepository.save(newUser);
        logger.info("User created successfully - User ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
        
        return savedUser;
    }

    /**
     * Update user by admin.
     * 
     * @param userId the user ID to update
     * @param updatedUser the updated user data
     * @param currentUser the current authenticated user
     * @return the updated user
     * @throws UnauthorizedAccessException if not admin
     * @throws UserNotFoundException if user not found
     */
    public User updateUserByAdmin(Long userId, User updatedUser, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can update user details");
        }

        User existingUser = getUserProfile(userId);
        
        // Validate input
        validateUserInput(updatedUser);

        // Update all editable fields
        if (StringUtils.hasText(updatedUser.getName())) {
            existingUser.setName(sanitizeInput(updatedUser.getName().trim()));
        }
        
        if (StringUtils.hasText(updatedUser.getDepartment())) {
            existingUser.setDepartment(sanitizeInput(updatedUser.getDepartment().trim()));
        }
        
        if (StringUtils.hasText(updatedUser.getPosition())) {
            existingUser.setPosition(sanitizeInput(updatedUser.getPosition().trim()));
        }
        
        if (StringUtils.hasText(updatedUser.getContactInfo())) {
            existingUser.setContactInfo(sanitizeInput(updatedUser.getContactInfo().trim()));
        }
        
        if (updatedUser.getRole() != null) {
            existingUser.setRole(updatedUser.getRole());
        }
        
        if (updatedUser.getJoinDate() != null) {
            existingUser.setJoinDate(updatedUser.getJoinDate());
        }

        return userRepository.save(existingUser);
    }

    /**
     * Change user password using DTO.
     * 
     * @param userId the user ID
     * @param changePasswordRequest the password change request
     * @param currentUser the current authenticated user
     * @throws UnauthorizedAccessException if user tries to change another user's password
     * @throws IllegalArgumentException if current password is incorrect
     */
    public void changePassword(Long userId, ChangePasswordRequest changePasswordRequest, User currentUser) {
        User user = getUserProfile(userId);
        
        // Check if user is changing their own password or if admin is changing
        if (!user.getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("You can only change your own password");
        }

        logger.info("Password change attempt - User ID: {}, Changed by: {}, Admin: {}", 
                   userId, currentUser.getId(), currentUser.isAdmin());

        // For non-admin users, verify current password
        if (!currentUser.isAdmin() && !passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            logger.warn("Password change failed - incorrect current password for User ID: {}", userId);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password (additional validation beyond DTO annotations)
        validatePassword(changePasswordRequest.getNewPassword());

        // Update password
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        
        logger.info("Password changed successfully - User ID: {}", userId);
    }

    /**
     * Change user password (legacy method for backward compatibility).
     * 
     * @param userId the user ID
     * @param currentPassword the current password
     * @param newPassword the new password
     * @param currentUser the current authenticated user
     * @throws UnauthorizedAccessException if user tries to change another user's password
     * @throws IllegalArgumentException if current password is incorrect
     */
    public void changePassword(Long userId, String currentPassword, String newPassword, User currentUser) {
        User user = getUserProfile(userId);
        
        // Check if user is changing their own password or if admin is changing
        if (!user.getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("You can only change your own password");
        }

        logger.info("Password change attempt - User ID: {}, Changed by: {}, Admin: {}", 
                   userId, currentUser.getId(), currentUser.isAdmin());

        // For non-admin users, verify current password
        if (!currentUser.isAdmin() && !passwordEncoder.matches(currentPassword, user.getPassword())) {
            logger.warn("Password change failed - incorrect current password for User ID: {}", userId);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password
        validatePassword(newPassword);

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        logger.info("Password changed successfully - User ID: {}", userId);
    }

    /**
     * Deactivate user (soft delete).
     * 
     * @param userId the user ID
     * @param currentUser the current authenticated user
     * @throws UnauthorizedAccessException if not admin
     * @throws UserNotFoundException if user not found
     */
    public void deactivateUser(Long userId, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can deactivate users");
        }

        User user = getUserProfile(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Reactivate user.
     * 
     * @param userId the user ID
     * @param currentUser the current authenticated user
     * @throws UnauthorizedAccessException if not admin
     * @throws UserNotFoundException if user not found
     */
    public void reactivateUser(Long userId, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can reactivate users");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }

        User user = userOpt.get();
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Get all active users as DTOs (admin only).
     * 
     * @param currentUser the current authenticated user
     * @return list of active user responses
     * @throws UnauthorizedAccessException if not admin
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsersResponse(User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can view all users");
        }
        return userRepository.findByActiveTrue().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active users (admin only) - legacy method for backward compatibility.
     * 
     * @param currentUser the current authenticated user
     * @return list of active users
     * @throws UnauthorizedAccessException if not admin
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers(User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can view all users");
        }
        return userRepository.findByActiveTrue();
    }

    /**
     * Get users with pagination as DTOs (admin only).
     * 
     * @param pageable pagination information
     * @param currentUser the current authenticated user
     * @return page of user responses
     * @throws UnauthorizedAccessException if not admin
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersWithPaginationResponse(Pageable pageable, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can view all users");
        }
        return userRepository.findByActive(true, pageable)
                .map(this::convertToUserResponse);
    }

    /**
     * Get users with pagination (admin only) - legacy method for backward compatibility.
     * 
     * @param pageable pagination information
     * @param currentUser the current authenticated user
     * @return page of users
     * @throws UnauthorizedAccessException if not admin
     */
    @Transactional(readOnly = true)
    public Page<User> getUsersWithPagination(Pageable pageable, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can view all users");
        }
        return userRepository.findByActive(true, pageable);
    }

    /**
     * Search users by name or email (admin only).
     * 
     * @param searchTerm the search term
     * @param pageable pagination information
     * @param currentUser the current authenticated user
     * @return page of matching users
     * @throws UnauthorizedAccessException if not admin
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, Pageable pageable, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can search users");
        }
        return userRepository.searchByNameOrEmail(searchTerm, true, pageable);
    }

    /**
     * Get users by role (admin only).
     * 
     * @param role the role to filter by
     * @param currentUser the current authenticated user
     * @return list of users with the specified role
     * @throws UnauthorizedAccessException if not admin
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only administrators can view users by role");
        }
        return userRepository.findByRoleAndActiveTrue(role);
    }

    /**
     * Validate user input data.
     * 
     * @param user the user to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUserInput(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User data cannot be null");
        }

        if (StringUtils.hasText(user.getName()) && user.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Name must not exceed 100 characters");
        }

        if (StringUtils.hasText(user.getDepartment()) && user.getDepartment().trim().length() > 100) {
            throw new IllegalArgumentException("Department must not exceed 100 characters");
        }

        if (StringUtils.hasText(user.getPosition()) && user.getPosition().trim().length() > 100) {
            throw new IllegalArgumentException("Position must not exceed 100 characters");
        }

        if (StringUtils.hasText(user.getContactInfo()) && user.getContactInfo().trim().length() > 200) {
            throw new IllegalArgumentException("Contact info must not exceed 200 characters");
        }
    }

    /**
     * Validate new user data for creation.
     * 
     * @param user the user to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateNewUserData(User user) {
        if (!StringUtils.hasText(user.getName())) {
            throw new IllegalArgumentException("Name is required");
        }

        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }

        validatePassword(user.getPassword());
        validateEmail(user.getEmail());
    }

    /**
     * Validate password strength.
     * 
     * @param password the password to validate
     * @throws IllegalArgumentException if password is weak
     */
    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        // Check for at least one special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }

    /**
     * Validate email format.
     * 
     * @param email the email to validate
     * @throws IllegalArgumentException if email is invalid
     */
    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }

        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (email.length() > 150) {
            throw new IllegalArgumentException("Email must not exceed 150 characters");
        }
    }

    /**
     * Validate user request input data.
     * 
     * @param userRequest the user request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUserRequestInput(UserRequest userRequest) {
        if (userRequest == null) {
            throw new IllegalArgumentException("User request data cannot be null");
        }

        if (StringUtils.hasText(userRequest.getName()) && userRequest.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Name must not exceed 100 characters");
        }

        if (StringUtils.hasText(userRequest.getDepartment()) && userRequest.getDepartment().trim().length() > 100) {
            throw new IllegalArgumentException("Department must not exceed 100 characters");
        }

        if (StringUtils.hasText(userRequest.getPosition()) && userRequest.getPosition().trim().length() > 100) {
            throw new IllegalArgumentException("Position must not exceed 100 characters");
        }

        if (StringUtils.hasText(userRequest.getContactInfo()) && userRequest.getContactInfo().trim().length() > 200) {
            throw new IllegalArgumentException("Contact info must not exceed 200 characters");
        }
    }

    /**
     * Validate new user request data for creation.
     * 
     * @param userRequest the user request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateNewUserRequestData(UserRequest userRequest) {
        if (!StringUtils.hasText(userRequest.getName())) {
            throw new IllegalArgumentException("Name is required");
        }

        if (!StringUtils.hasText(userRequest.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!StringUtils.hasText(userRequest.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }

        validatePassword(userRequest.getPassword());
        validateEmail(userRequest.getEmail());
    }

    /**
     * Validate user profile update request input data.
     * 
     * @param updateRequest the user profile update request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUserProfileUpdateInput(UserProfileUpdateRequest updateRequest) {
        if (updateRequest == null) {
            throw new IllegalArgumentException("User profile update request data cannot be null");
        }

        if (StringUtils.hasText(updateRequest.getName()) && updateRequest.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Name must not exceed 100 characters");
        }

        if (StringUtils.hasText(updateRequest.getDepartment()) && updateRequest.getDepartment().trim().length() > 100) {
            throw new IllegalArgumentException("Department must not exceed 100 characters");
        }

        if (StringUtils.hasText(updateRequest.getPosition()) && updateRequest.getPosition().trim().length() > 100) {
            throw new IllegalArgumentException("Position must not exceed 100 characters");
        }

        if (StringUtils.hasText(updateRequest.getContactInfo()) && updateRequest.getContactInfo().trim().length() > 200) {
            throw new IllegalArgumentException("Contact info must not exceed 200 characters");
        }

        if (StringUtils.hasText(updateRequest.getEmail())) {
            validateEmail(updateRequest.getEmail());
        }
    }

    /**
     * Convert User entity to UserResponse DTO.
     * 
     * @param user the user entity
     * @return the user response DTO
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
     * Convert User entity to UserProfileResponse DTO.
     * 
     * @param user the user entity
     * @return the user profile response DTO
     */
    private UserProfileResponse convertToUserProfileResponse(User user) {
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
     * Sanitize input to prevent XSS attacks.
     * 
     * @param input the input to sanitize
     * @return sanitized input
     */
    private String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        return input.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("/", "&#x2F;");
    }
}