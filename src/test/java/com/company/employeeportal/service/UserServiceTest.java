package com.company.employeeportal.service;

import com.company.employeeportal.dto.*;
import com.company.employeeportal.exception.EmailAlreadyExistsException;
import com.company.employeeportal.exception.UnauthorizedAccessException;
import com.company.employeeportal.exception.UserNotFoundException;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testEmployee;
    private User testAdmin;
    private User testManager;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setName("John Doe");
        testEmployee.setEmail("john.doe@company.com");
        testEmployee.setPassword("encodedPassword");
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setDepartment("IT");
        testEmployee.setPosition("Developer");
        testEmployee.setActive(true);

        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setName("Admin User");
        testAdmin.setEmail("admin@company.com");
        testAdmin.setPassword("encodedPassword");
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setActive(true);

        testManager = new User();
        testManager.setId(3L);
        testManager.setName("Manager User");
        testManager.setEmail("manager@company.com");
        testManager.setPassword("encodedPassword");
        testManager.setRole(Role.MANAGER);
        testManager.setActive(true);
    }

    @Test
    void getUserProfile_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // When
        User result = userService.getUserProfile(1L);

        // Then
        assertNotNull(result);
        assertEquals(testEmployee.getId(), result.getId());
        assertEquals(testEmployee.getName(), result.getName());
        assertEquals(testEmployee.getEmail(), result.getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserProfile_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.getUserProfile(1L));
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserProfile_WhenUserInactive_ShouldThrowException() {
        // Given
        testEmployee.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.getUserProfile(1L));
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findByEmailAndActive("john.doe@company.com", true))
                .thenReturn(Optional.of(testEmployee));

        // When
        User result = userService.getUserByEmail("john.doe@company.com");

        // Then
        assertNotNull(result);
        assertEquals(testEmployee.getEmail(), result.getEmail());
        verify(userRepository).findByEmailAndActive("john.doe@company.com", true);
    }

    @Test
    void updateUserProfile_WhenUserUpdatesOwnProfile_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        User updatedData = new User();
        updatedData.setName("John Updated");
        updatedData.setContactInfo("123-456-7890");

        // When
        User result = userService.updateUserProfile(1L, updatedData, testEmployee);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testEmployee);
        assertEquals("John Updated", testEmployee.getName());
        assertEquals("123-456-7890", testEmployee.getContactInfo());
    }

    @Test
    void updateUserProfile_WhenNonAdminUpdatesOtherProfile_ShouldThrowException() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(testAdmin));

        User updatedData = new User();
        updatedData.setName("Updated Name");

        // When & Then
        assertThrows(UnauthorizedAccessException.class, 
                () -> userService.updateUserProfile(2L, updatedData, testEmployee));
        verify(userRepository).findById(2L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenAdminCreatesUser_ShouldCreateSuccessfully() {
        // Given
        when(userRepository.existsByEmail("new.user@company.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("new.user@company.com");
        newUser.setPassword("Password123!");
        newUser.setRole(Role.EMPLOYEE);

        // When
        User result = userService.createUser(newUser, testAdmin);

        // Then
        assertNotNull(result);
        verify(userRepository).existsByEmail("new.user@company.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WhenNonAdminCreatesUser_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("new.user@company.com");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(UnauthorizedAccessException.class, 
                () -> userService.createUser(newUser, testEmployee));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenEmailAlreadyExists_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail("existing@company.com")).thenReturn(true);

        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("existing@company.com");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(EmailAlreadyExistsException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository).existsByEmail("existing@company.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_WhenUserChangesOwnPassword_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        // When
        userService.changePassword(1L, "currentPassword", "NewPassword123!", testEmployee);

        // Then
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPassword", "encodedPassword");
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(testEmployee);
    }

    @Test
    void changePassword_WhenCurrentPasswordIncorrect_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.changePassword(1L, "wrongPassword", "NewPassword123!", testEmployee));
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_WhenAdminResetsPassword_ShouldUpdateWithoutCurrentPassword() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        // When
        userService.changePassword(1L, null, "NewPassword123!", testAdmin);

        // Then
        verify(userRepository).findById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(testEmployee);
    }

    @Test
    void deactivateUser_WhenAdminDeactivatesUser_ShouldDeactivateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        // When
        userService.deactivateUser(1L, testAdmin);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(testEmployee);
        assertFalse(testEmployee.getActive());
    }

    @Test
    void deactivateUser_WhenNonAdminDeactivatesUser_ShouldThrowException() {
        // When & Then
        assertThrows(UnauthorizedAccessException.class, 
                () -> userService.deactivateUser(1L, testEmployee));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAllActiveUsers_WhenAdminRequests_ShouldReturnUsers() {
        // Given
        List<User> users = Arrays.asList(testEmployee, testManager);
        when(userRepository.findByActiveTrue()).thenReturn(users);

        // When
        List<User> result = userService.getAllActiveUsers(testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findByActiveTrue();
    }

    @Test
    void getAllActiveUsers_WhenNonAdminRequests_ShouldThrowException() {
        // When & Then
        assertThrows(UnauthorizedAccessException.class, 
                () -> userService.getAllActiveUsers(testEmployee));
        verify(userRepository, never()).findByActiveTrue();
    }

    @Test
    void getUsersWithPagination_WhenAdminRequests_ShouldReturnPagedUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testEmployee, testManager);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findByActive(true, pageable)).thenReturn(userPage);

        // When
        Page<User> result = userService.getUsersWithPagination(pageable, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(userRepository).findByActive(true, pageable);
    }

    @Test
    void searchUsers_WhenAdminSearches_ShouldReturnMatchingUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testEmployee);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        when(userRepository.searchByNameOrEmail("John", true, pageable)).thenReturn(userPage);

        // When
        Page<User> result = userService.searchUsers("John", pageable, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(userRepository).searchByNameOrEmail("John", true, pageable);
    }

    @Test
    void getUsersByRole_WhenAdminRequests_ShouldReturnUsersByRole() {
        // Given
        List<User> employees = Arrays.asList(testEmployee);
        when(userRepository.findByRoleAndActiveTrue(Role.EMPLOYEE)).thenReturn(employees);

        // When
        List<User> result = userService.getUsersByRole(Role.EMPLOYEE, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Role.EMPLOYEE, result.get(0).getRole());
        verify(userRepository).findByRoleAndActiveTrue(Role.EMPLOYEE);
    }

    @Test
    void validatePassword_WhenWeakPassword_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("weak"); // Weak password

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
    }

    @Test
    void validateEmail_WhenInvalidEmail_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("invalid-email"); // Invalid email format
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
    }

    @Test
    void sanitizeInput_WhenInputContainsXSS_ShouldSanitize() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        User updatedData = new User();
        updatedData.setName("<script>alert('xss')</script>John");
        updatedData.setContactInfo("Contact<script>alert('xss')</script>");

        // When
        userService.updateUserProfile(1L, updatedData, testEmployee);

        // Then
        verify(userRepository).save(testEmployee);
        // The name should be sanitized (XSS characters escaped)
        assertTrue(testEmployee.getName().contains("&lt;script&gt;"));
        assertFalse(testEmployee.getName().contains("<script>"));
    }

    // DTO-based method tests

    @Test
    void getUserProfileResponse_WhenUserExists_ShouldReturnUserProfileResponse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // When
        UserProfileResponse result = userService.getUserProfileResponse(1L);

        // Then
        assertNotNull(result);
        assertEquals(testEmployee.getId(), result.getId());
        assertEquals(testEmployee.getName(), result.getName());
        assertEquals(testEmployee.getEmail(), result.getEmail());
        assertEquals(testEmployee.getRole(), result.getRole());
        assertEquals(testEmployee.getDepartment(), result.getDepartment());
        assertEquals(testEmployee.getPosition(), result.getPosition());
        verify(userRepository).findById(1L);
    }

    @Test
    void updateUserProfileWithDTO_WhenUserUpdatesOwnProfile_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        UserRequest userRequest = new UserRequest();
        userRequest.setName("John Updated");
        userRequest.setContactInfo("123-456-7890");

        // When
        UserProfileResponse result = userService.updateUserProfile(1L, userRequest, testEmployee);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testEmployee);
        assertEquals("John Updated", testEmployee.getName());
        assertEquals("123-456-7890", testEmployee.getContactInfo());
        assertEquals("John Updated", result.getName());
        assertEquals("123-456-7890", result.getContactInfo());
    }

    @Test
    void createUserWithDTO_WhenAdminCreatesUser_ShouldCreateSuccessfully() {
        // Given
        when(userRepository.existsByEmail("new.user@company.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setId(4L);
        savedUser.setName("New User");
        savedUser.setEmail("new.user@company.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(Role.EMPLOYEE);
        savedUser.setActive(true);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserRequest userRequest = new UserRequest();
        userRequest.setName("New User");
        userRequest.setEmail("new.user@company.com");
        userRequest.setPassword("Password123!");
        userRequest.setRole(Role.EMPLOYEE);

        // When
        UserResponse result = userService.createUser(userRequest, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(4L, result.getId());
        assertEquals("New User", result.getName());
        assertEquals("new.user@company.com", result.getEmail());
        assertEquals(Role.EMPLOYEE, result.getRole());
        assertTrue(result.getActive());
        verify(userRepository).existsByEmail("new.user@company.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePasswordWithDTO_WhenUserChangesOwnPassword_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("currentPassword");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");

        // When
        userService.changePassword(1L, changePasswordRequest, testEmployee);

        // Then
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPassword", "encodedPassword");
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(testEmployee);
    }

    @Test
    void getAllActiveUsersResponse_WhenAdminRequests_ShouldReturnUserResponses() {
        // Given
        List<User> users = Arrays.asList(testEmployee, testManager);
        when(userRepository.findByActiveTrue()).thenReturn(users);

        // When
        List<UserResponse> result = userService.getAllActiveUsersResponse(testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testEmployee.getId(), result.get(0).getId());
        assertEquals(testEmployee.getName(), result.get(0).getName());
        assertEquals(testManager.getId(), result.get(1).getId());
        assertEquals(testManager.getName(), result.get(1).getName());
        verify(userRepository).findByActiveTrue();
    }

    @Test
    void getUsersWithPaginationResponse_WhenAdminRequests_ShouldReturnPagedUserResponses() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(testEmployee, testManager);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findByActive(true, pageable)).thenReturn(userPage);

        // When
        Page<UserResponse> result = userService.getUsersWithPaginationResponse(pageable, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(testEmployee.getId(), result.getContent().get(0).getId());
        assertEquals(testEmployee.getName(), result.getContent().get(0).getName());
        verify(userRepository).findByActive(true, pageable);
    }

    // Additional comprehensive tests for password encryption

    @Test
    void createUser_ShouldEncryptPasswordProperly() {
        // Given
        String plainPassword = "Password123!";
        String encodedPassword = "encodedPassword123";
        
        when(userRepository.existsByEmail("test@company.com")).thenReturn(false);
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);
        
        User savedUser = new User();
        savedUser.setId(5L);
        savedUser.setName("Test User");
        savedUser.setEmail("test@company.com");
        savedUser.setPassword(encodedPassword);
        savedUser.setRole(Role.EMPLOYEE);
        savedUser.setActive(true);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword(plainPassword);

        // When
        User result = userService.createUser(newUser, testAdmin);

        // Then
        verify(passwordEncoder).encode(plainPassword);
        assertEquals(encodedPassword, result.getPassword());
        assertNotEquals(plainPassword, result.getPassword());
    }

    @Test
    void changePassword_ShouldEncryptNewPasswordProperly() {
        // Given
        String currentPassword = "oldPassword";
        String newPassword = "NewPassword123!";
        String encodedNewPassword = "encodedNewPassword";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches(currentPassword, testEmployee.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        // When
        userService.changePassword(1L, currentPassword, newPassword, testEmployee);

        // Then
        verify(passwordEncoder).encode(newPassword);
        assertEquals(encodedNewPassword, testEmployee.getPassword());
        assertNotEquals(newPassword, testEmployee.getPassword());
    }

    // Comprehensive validation tests

    @Test
    void createUser_WhenNameTooLong_ShouldThrowException() {
        // Given
        String longName = "a".repeat(101); // 101 characters
        User newUser = new User();
        newUser.setName(longName);
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenDepartmentTooLong_ShouldThrowException() {
        // Given
        String longDepartment = "a".repeat(101); // 101 characters
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");
        newUser.setDepartment(longDepartment);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenPositionTooLong_ShouldThrowException() {
        // Given
        String longPosition = "a".repeat(101); // 101 characters
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");
        newUser.setPosition(longPosition);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenContactInfoTooLong_ShouldThrowException() {
        // Given
        String longContactInfo = "a".repeat(201); // 201 characters
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");
        newUser.setContactInfo(longContactInfo);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenEmailTooLong_ShouldThrowException() {
        // Given
        String longEmail = "a".repeat(140) + "@company.com"; // > 150 characters
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail(longEmail);
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenNameIsEmpty_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenEmailIsEmpty_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenPasswordIsEmpty_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    // Password validation tests

    @Test
    void validatePassword_WhenPasswordTooShort_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Pass1!"); // Only 6 characters

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void validatePassword_WhenPasswordMissingUppercase_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("password123!"); // No uppercase

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void validatePassword_WhenPasswordMissingLowercase_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("PASSWORD123!"); // No lowercase

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void validatePassword_WhenPasswordMissingDigit_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password!"); // No digit

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void validatePassword_WhenPasswordMissingSpecialChar_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123"); // No special character

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    // Email validation tests

    @Test
    void validateEmail_WhenEmailInvalidFormat_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("invalid-email-format");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void validateEmail_WhenEmailMissingAtSymbol_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("testcompany.com");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(newUser, testAdmin));
        verify(userRepository, never()).save(any());
    }

    // Input sanitization tests

    @Test
    void updateUserProfile_WhenInputContainsXSSInName_ShouldSanitize() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        User updatedData = new User();
        updatedData.setName("<script>alert('xss')</script>John");

        // When
        userService.updateUserProfile(1L, updatedData, testEmployee);

        // Then
        verify(userRepository).save(testEmployee);
        assertTrue(testEmployee.getName().contains("&lt;script&gt;"));
        assertFalse(testEmployee.getName().contains("<script>"));
    }

    @Test
    void updateUserProfile_WhenInputContainsXSSInDepartment_ShouldSanitize() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        User updatedData = new User();
        updatedData.setDepartment("<img src=x onerror=alert('xss')>IT");

        // When
        userService.updateUserProfile(1L, updatedData, testAdmin); // Admin can update department

        // Then
        verify(userRepository).save(testEmployee);
        assertTrue(testEmployee.getDepartment().contains("&lt;img"));
        assertFalse(testEmployee.getDepartment().contains("<img"));
    }

    @Test
    void createUser_WhenInputContainsXSS_ShouldSanitize() {
        // Given
        when(userRepository.existsByEmail("test@company.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setId(6L);
        savedUser.setName("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;&#x2F;script&gt;Test User");
        savedUser.setEmail("test@company.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(Role.EMPLOYEE);
        savedUser.setActive(true);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User newUser = new User();
        newUser.setName("<script>alert('xss')</script>Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");

        // When
        User result = userService.createUser(newUser, testAdmin);

        // Then
        verify(userRepository).save(any(User.class));
        assertTrue(result.getName().contains("&lt;script&gt;"));
        assertFalse(result.getName().contains("<script>"));
    }

    // Error handling tests for null inputs

    @Test
    void updateUserProfile_WhenUserDataIsNull_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.updateUserProfile(1L, (User) null, testEmployee));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenUserDataIsNull_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser((User) null, testAdmin));
        verify(userRepository, never()).save(any());
    }

    // DTO validation tests

    @Test
    void updateUserProfileWithDTO_WhenRequestIsNull_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.updateUserProfile(1L, (UserRequest) null, testEmployee));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserWithDTO_WhenRequestIsNull_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser((UserRequest) null, testAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordWithDTO_WhenWeakNewPassword_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("currentPassword");
        changePasswordRequest.setNewPassword("weak"); // Weak password
        changePasswordRequest.setConfirmPassword("weak");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> userService.changePassword(1L, changePasswordRequest, testEmployee));
        verify(userRepository, never()).save(any());
    }

    // Admin authorization tests

    @Test
    void updateUserByAdmin_WhenNonAdminUpdatesUser_ShouldThrowException() {
        // When & Then
        assertThrows(UnauthorizedAccessException.class, 
                () -> userService.updateUserByAdmin(1L, new User(), testEmployee));
        verify(userRepository, never()).save(any());
    }

    @Test
    void reactivateUser_WhenNonAdminReactivatesUser_ShouldThrowException() {
        // When & Then
        assertThrows(UnauthorizedAccessException.class, 
                () -> userService.reactivateUser(1L, testEmployee));
        verify(userRepository, never()).save(any());
    }

    @Test
    void reactivateUser_WhenAdminReactivatesUser_ShouldReactivateSuccessfully() {
        // Given
        User inactiveUser = new User();
        inactiveUser.setId(1L);
        inactiveUser.setActive(false);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenReturn(inactiveUser);

        // When
        userService.reactivateUser(1L, testAdmin);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(inactiveUser);
        assertTrue(inactiveUser.getActive());
    }

    // Edge case tests

    @Test
    void updateUserProfile_WhenOnlyWhitespaceInName_ShouldNotUpdate() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        String originalName = testEmployee.getName();
        User updatedData = new User();
        updatedData.setName("   "); // Only whitespace

        // When
        userService.updateUserProfile(1L, updatedData, testEmployee);

        // Then
        verify(userRepository).save(testEmployee);
        assertEquals(originalName, testEmployee.getName()); // Name should remain unchanged
    }

    @Test
    void createUser_WhenDefaultValuesSet_ShouldSetCorrectDefaults() {
        // Given
        when(userRepository.existsByEmail("test@company.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setId(7L);
        savedUser.setName("Test User");
        savedUser.setEmail("test@company.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(Role.EMPLOYEE);
        savedUser.setActive(true);
        savedUser.setJoinDate(LocalDate.now());
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");
        // No role or joinDate set

        // When
        User result = userService.createUser(newUser, testAdmin);

        // Then
        assertEquals(Role.EMPLOYEE, result.getRole());
        assertTrue(result.getActive());
        assertNotNull(result.getJoinDate());
    }

    // Additional tests for comprehensive coverage of user creation and update logic

    @Test
    void updateUserByAdmin_WhenAdminUpdatesAllFields_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        User updatedData = new User();
        updatedData.setName("Updated Name");
        updatedData.setDepartment("Updated Department");
        updatedData.setPosition("Updated Position");
        updatedData.setContactInfo("Updated Contact");
        updatedData.setRole(Role.MANAGER);
        updatedData.setJoinDate(LocalDate.of(2023, 1, 1));

        // When
        User result = userService.updateUserByAdmin(1L, updatedData, testAdmin);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testEmployee);
        assertEquals("Updated Name", testEmployee.getName());
        assertEquals("Updated Department", testEmployee.getDepartment());
        assertEquals("Updated Position", testEmployee.getPosition());
        assertEquals("Updated Contact", testEmployee.getContactInfo());
        assertEquals(Role.MANAGER, testEmployee.getRole());
        assertEquals(LocalDate.of(2023, 1, 1), testEmployee.getJoinDate());
    }

    @Test
    void createUserWithDTO_WhenAllValidationPasses_ShouldCreateWithAllFields() {
        // Given
        when(userRepository.existsByEmail("complete@company.com")).thenReturn(false);
        when(passwordEncoder.encode("CompletePassword123!")).thenReturn("encodedCompletePassword");
        
        User savedUser = new User();
        savedUser.setId(8L);
        savedUser.setName("Complete User");
        savedUser.setEmail("complete@company.com");
        savedUser.setPassword("encodedCompletePassword");
        savedUser.setRole(Role.MANAGER);
        savedUser.setDepartment("HR");
        savedUser.setPosition("HR Manager");
        savedUser.setContactInfo("555-0123");
        savedUser.setJoinDate(LocalDate.of(2024, 1, 15));
        savedUser.setActive(true);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserRequest userRequest = new UserRequest();
        userRequest.setName("Complete User");
        userRequest.setEmail("complete@company.com");
        userRequest.setPassword("CompletePassword123!");
        userRequest.setRole(Role.MANAGER);
        userRequest.setDepartment("HR");
        userRequest.setPosition("HR Manager");
        userRequest.setContactInfo("555-0123");
        userRequest.setJoinDate(LocalDate.of(2024, 1, 15));

        // When
        UserResponse result = userService.createUser(userRequest, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals(8L, result.getId());
        assertEquals("Complete User", result.getName());
        assertEquals("complete@company.com", result.getEmail());
        assertEquals(Role.MANAGER, result.getRole());
        assertEquals("HR", result.getDepartment());
        assertEquals("HR Manager", result.getPosition());
        assertEquals("555-0123", result.getContactInfo());
        assertEquals(LocalDate.of(2024, 1, 15), result.getJoinDate());
        assertTrue(result.getActive());
        
        verify(userRepository).existsByEmail("complete@company.com");
        verify(passwordEncoder).encode("CompletePassword123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_WhenAdminUpdatesRestrictedFields_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        UserRequest userRequest = new UserRequest();
        userRequest.setName("Admin Updated Name");
        userRequest.setDepartment("Admin Updated Department");
        userRequest.setPosition("Admin Updated Position");
        userRequest.setRole(Role.MANAGER);
        userRequest.setJoinDate(LocalDate.of(2023, 6, 1));

        // When
        UserProfileResponse result = userService.updateUserProfile(1L, userRequest, testAdmin);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testEmployee);
        assertEquals("Admin Updated Name", testEmployee.getName());
        assertEquals("Admin Updated Department", testEmployee.getDepartment());
        assertEquals("Admin Updated Position", testEmployee.getPosition());
        assertEquals(Role.MANAGER, testEmployee.getRole());
        assertEquals(LocalDate.of(2023, 6, 1), testEmployee.getJoinDate());
    }

    @Test
    void updateUserProfile_WhenEmployeeUpdatesRestrictedFields_ShouldIgnoreRestrictedFields() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testEmployee);

        String originalDepartment = testEmployee.getDepartment();
        String originalPosition = testEmployee.getPosition();
        Role originalRole = testEmployee.getRole();
        LocalDate originalJoinDate = testEmployee.getJoinDate();

        UserRequest userRequest = new UserRequest();
        userRequest.setName("Employee Updated Name");
        userRequest.setContactInfo("Employee Updated Contact");
        userRequest.setDepartment("Attempted Department Change");
        userRequest.setPosition("Attempted Position Change");
        userRequest.setRole(Role.ADMIN);
        userRequest.setJoinDate(LocalDate.of(2020, 1, 1));

        // When
        UserProfileResponse result = userService.updateUserProfile(1L, userRequest, testEmployee);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testEmployee);
        
        // Editable fields should be updated
        assertEquals("Employee Updated Name", testEmployee.getName());
        assertEquals("Employee Updated Contact", testEmployee.getContactInfo());
        
        // Restricted fields should remain unchanged
        assertEquals(originalDepartment, testEmployee.getDepartment());
        assertEquals(originalPosition, testEmployee.getPosition());
        assertEquals(originalRole, testEmployee.getRole());
        assertEquals(originalJoinDate, testEmployee.getJoinDate());
    }

    // Test password encryption in different scenarios

    @Test
    void createUserWithDTO_ShouldAlwaysEncryptPassword() {
        // Given
        String[] testPasswords = {
            "SimplePass123!",
            "Complex@Password456#",
            "Another$Strong789%"
        };
        
        for (int i = 0; i < testPasswords.length; i++) {
            String plainPassword = testPasswords[i];
            String encodedPassword = "encoded" + i;
            String email = "test" + i + "@company.com";
            
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);
            
            User savedUser = new User();
            savedUser.setId((long) (i + 10));
            savedUser.setName("Test User " + i);
            savedUser.setEmail(email);
            savedUser.setPassword(encodedPassword);
            savedUser.setRole(Role.EMPLOYEE);
            savedUser.setActive(true);
            
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserRequest userRequest = new UserRequest();
            userRequest.setName("Test User " + i);
            userRequest.setEmail(email);
            userRequest.setPassword(plainPassword);

            // When
            UserResponse result = userService.createUser(userRequest, testAdmin);

            // Then
            verify(passwordEncoder).encode(plainPassword);
            assertNotEquals(plainPassword, result.getId()); // Password should not be in response
            
            // Reset mocks for next iteration
            reset(userRepository, passwordEncoder);
        }
    }

    // Test comprehensive error handling scenarios

    @Test
    void createUser_WhenRepositorySaveThrowsException_ShouldPropagateException() {
        // Given
        when(userRepository.existsByEmail("test@company.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@company.com");
        newUser.setPassword("Password123!");

        // When & Then
        assertThrows(RuntimeException.class, 
                () -> userService.createUser(newUser, testAdmin));
        
        verify(userRepository).existsByEmail("test@company.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_WhenRepositoryFindThrowsException_ShouldPropagateException() {
        // Given
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database connection error"));

        User updatedData = new User();
        updatedData.setName("Updated Name");

        // When & Then
        assertThrows(RuntimeException.class, 
                () -> userService.updateUserProfile(1L, updatedData, testEmployee));
        
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }