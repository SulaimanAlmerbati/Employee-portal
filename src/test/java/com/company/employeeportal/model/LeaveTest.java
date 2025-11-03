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

class LeaveTest {

    private Validator validator;
    private Leave leave;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@array.world");
        user.setRole(Role.EMPLOYEE);
        
        leave = new Leave();
        leave.setUser(user);
        leave.setLeaveType(LeaveType.ANNUAL);
        leave.setStartDate(LocalDate.now().plusDays(1));
        leave.setEndDate(LocalDate.now().plusDays(3));
        leave.setReason("Vacation");
    }

    @Test
    void testValidLeave() {
        Set<ConstraintViolation<Leave>> violations = validator.validate(leave);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidEndDate() {
        leave.setEndDate(LocalDate.now().minusDays(1)); // End date before start date
        Set<ConstraintViolation<Leave>> violations = validator.validate(leave);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("End date must be after or equal to start date")));
    }

    @Test
    void testBlankReason() {
        leave.setReason("");
        Set<ConstraintViolation<Leave>> violations = validator.validate(leave);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Reason is required")));
    }

    @Test
    void testNullUser() {
        leave.setUser(null);
        Set<ConstraintViolation<Leave>> violations = validator.validate(leave);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("User is required")));
    }

    @Test
    void testStatusHelperMethods() {
        leave.setStatus(LeaveStatus.PENDING);
        assertTrue(leave.isPending());
        assertFalse(leave.isApproved());
        assertFalse(leave.isRejected());

        leave.setStatus(LeaveStatus.APPROVED);
        assertFalse(leave.isPending());
        assertTrue(leave.isApproved());
        assertFalse(leave.isRejected());

        leave.setStatus(LeaveStatus.REJECTED);
        assertFalse(leave.isPending());
        assertFalse(leave.isApproved());
        assertTrue(leave.isRejected());
    }

    @Test
    void testDurationCalculation() {
        leave.setStartDate(LocalDate.of(2024, 1, 1));
        leave.setEndDate(LocalDate.of(2024, 1, 3));
        assertEquals(3, leave.getDurationInDays());
    }

    @Test
    void testDefaultStatus() {
        Leave newLeave = new Leave();
        assertEquals(LeaveStatus.PENDING, newLeave.getStatus());
    }

    @Test
    void testConstructorWithParameters() {
        Leave newLeave = new Leave(user, LeaveType.SICK, LocalDate.now().plusDays(1), 
                                   LocalDate.now().plusDays(2), "Illness");
        assertEquals(user, newLeave.getUser());
        assertEquals(LeaveType.SICK, newLeave.getLeaveType());
        assertEquals("Illness", newLeave.getReason());
        assertEquals(LeaveStatus.PENDING, newLeave.getStatus());
    }

    @Test
    void testEntityRelationships() {
        // Test user relationship
        assertNotNull(leave.getUser());
        assertEquals(user, leave.getUser());
        
        // Test approvedBy relationship (initially null)
        assertNull(leave.getApprovedBy());
        
        User manager = new User();
        manager.setId(2L);
        manager.setRole(Role.MANAGER);
        leave.setApprovedBy(manager);
        assertEquals(manager, leave.getApprovedBy());
    }

    @Test
    void testAuditFields() {
        // Test that audit fields are properly configured
        assertNull(leave.getCreatedAt()); // Will be set by @CreationTimestamp
        assertNull(leave.getUpdatedAt()); // Will be set by @UpdateTimestamp
    }

    @Test
    void testCustomValidationWithValidDates() {
        leave.setStartDate(LocalDate.now().plusDays(1));
        leave.setEndDate(LocalDate.now().plusDays(3));
        assertTrue(leave.isEndDateValid());
    }

    @Test
    void testCustomValidationWithNullDates() {
        leave.setStartDate(null);
        leave.setEndDate(null);
        assertTrue(leave.isEndDateValid()); // Should be valid when both are null
    }

    @Test
    void testToString() {
        leave.setId(1L);
        String toString = leave.toString();
        assertTrue(toString.contains("Leave{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("leaveType=ANNUAL"));
        assertTrue(toString.contains("status=PENDING"));
        assertTrue(toString.contains("user=John Doe"));
    }
}
