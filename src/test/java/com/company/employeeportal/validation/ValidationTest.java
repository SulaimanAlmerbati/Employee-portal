package com.company.employeeportal.validation;

import com.company.employeeportal.dto.UserRequest;
import com.company.employeeportal.dto.ChangePasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for custom validation annotations.
 */
@SpringBootTest
public class ValidationTest {

    @Autowired
    private Validator validator;

    @Test
    void testValidNameValidator_ValidName_ShouldPass() {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("John Doe");
        userRequest.setEmail("john.doe@array.world");
        userRequest.setPassword("Password123!");

        // When
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(userRequest);

        // Then
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void testValidNameValidator_InvalidName_ShouldFail() {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("John123"); // Contains numbers
        userRequest.setEmail("john.doe@array.world");
        userRequest.setPassword("Password123!");

        // When
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(userRequest);

        // Then
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("name") &&
            v.getMessage().contains("Name must contain only letters, spaces, hyphens, and apostrophes")));
    }

    @Test
    void testValidContactInfoValidator_ValidPhone_ShouldPass() {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("John Doe");
        userRequest.setEmail("john.doe@array.world");
        userRequest.setPassword("Password123!");
        userRequest.setContactInfo("123-456-7890");

        // When
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(userRequest);

        // Then
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("contactInfo")));
    }

    @Test
    void testValidContactInfoValidator_ValidEmail_ShouldPass() {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("John Doe");
        userRequest.setEmail("john.doe@array.world");
        userRequest.setPassword("Password123!");
        userRequest.setContactInfo("contact@example.com");

        // When
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(userRequest);

        // Then
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("contactInfo")));
    }

    @Test
    void testStrongPasswordValidator_ValidPassword_ShouldPass() {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("John Doe");
        userRequest.setEmail("john.doe@array.world");
        userRequest.setPassword("Password123!");

        // When
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(userRequest);

        // Then
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void testStrongPasswordValidator_WeakPassword_ShouldFail() {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("John Doe");
        userRequest.setEmail("john.doe@array.world");
        userRequest.setPassword("password"); // No uppercase, no numbers, no special chars

        // When
        Set<ConstraintViolation<UserRequest>> violations = validator.validate(userRequest);

        // Then
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void testPasswordMatchesValidator_MatchingPasswords_ShouldPass() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword123!");
        request.setNewPassword("NewPassword123!");
        request.setConfirmPassword("NewPassword123!");

        // When
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.stream().noneMatch(v -> 
            v.getMessage().contains("Password and confirm password do not match")));
    }

    @Test
    void testPasswordMatchesValidator_NonMatchingPasswords_ShouldFail() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword123!");
        request.setNewPassword("NewPassword123!");
        request.setConfirmPassword("DifferentPassword123!");

        // When
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("Password and confirm password do not match")));
    }
}
