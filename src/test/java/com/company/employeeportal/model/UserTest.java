package com.company.employeeportal.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private Validator validator;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        user = new User();
        user.setName("John Doe");
        user.setEmail("john.doe@company.com");
        user.setPassword("password123");
        user.setRole(Role.EMPLOYEE);
        user.setDepartment("IT");
        user.setPosition("Developer");
        user.setJoinDate(LocalDate.now());
    }

    @Test
    void testValidUser() {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidEmail() {
        user.setEmail("invalid-email");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Email must be valid")));
    }

    @Test
    void testBlankName() {
        user.setName("");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name is required")));
    }

    @Test
    void testNullRole() {
        user.setRole(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Role is required")));
    }

    @Test
    void testRoleHelperMethods() {
        user.setRole(Role.ADMIN);
        assertTrue(user.isAdmin());
        assertTrue(user.isManager());
        assertFalse(user.isEmployee());

        user.setRole(Role.MANAGER);
        assertFalse(user.isAdmin());
        assertTrue(user.isManager());
        assertFalse(user.isEmployee());

        user.setRole(Role.EMPLOYEE);
        assertFalse(user.isAdmin());
        assertFalse(user.isManager());
        assertTrue(user.isEmployee());
    }

    @Test
    void testDefaultActiveStatus() {
        User newUser = new User();
        assertTrue(newUser.getActive());
    }

    @Test
    void testConstructorWithParameters() {
        User newUser = new User("Jane Smith", "jane@company.com", "password456", Role.MANAGER);
        assertEquals("Jane Smith", newUser.getName());
        assertEquals("jane@company.com", newUser.getEmail());
        assertEquals("password456", newUser.getPassword());
        assertEquals(Role.MANAGER, newUser.getRole());
    }

    @Test
    void testEntityRelationships() {
        // Test bidirectional relationships initialization
        assertNotNull(user.getLeaves());
        assertNotNull(user.getPayrolls());
        assertNotNull(user.getAnnouncements());
        assertNotNull(user.getApprovedLeaves());
        
        assertTrue(user.getLeaves().isEmpty());
        assertTrue(user.getPayrolls().isEmpty());
        assertTrue(user.getAnnouncements().isEmpty());
        assertTrue(user.getApprovedLeaves().isEmpty());
    }

    @Test
    void testAuditFields() {
        // Test that audit fields are properly configured
        assertNull(user.getCreatedAt()); // Will be set by @CreationTimestamp
        assertNull(user.getUpdatedAt()); // Will be set by @UpdateTimestamp
    }

    @Test
    void testToString() {
        user.setId(1L);
        String toString = user.toString();
        assertTrue(toString.contains("User{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("name='John Doe'"));
        assertTrue(toString.contains("email='john.doe@company.com'"));
        assertTrue(toString.contains("role=EMPLOYEE"));
        assertTrue(toString.contains("active=true"));
    }
}