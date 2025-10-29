package com.company.employeeportal.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PayrollTest {

    private Validator validator;
    private Payroll payroll;
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
        
        payroll = new Payroll();
        payroll.setUser(user);
        payroll.setMonth(1);
        payroll.setYear(2024);
        payroll.setGrossPay(new BigDecimal("5000.00"));
        payroll.setNetPay(new BigDecimal("4000.00"));
        payroll.setDeductions(new BigDecimal("500.00"));
    }

    @Test
    void testValidPayroll() {
        Set<ConstraintViolation<Payroll>> violations = validator.validate(payroll);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidMonth() {
        payroll.setMonth(13);
        Set<ConstraintViolation<Payroll>> violations = validator.validate(payroll);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Month must be between 1 and 12")));
    }

    @Test
    void testInvalidYear() {
        payroll.setYear(2019);
        Set<ConstraintViolation<Payroll>> violations = validator.validate(payroll);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Year must be 2020 or later")));
    }

    @Test
    void testNegativeGrossPay() {
        payroll.setGrossPay(new BigDecimal("-100.00"));
        Set<ConstraintViolation<Payroll>> violations = validator.validate(payroll);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Gross pay must be non-negative")));
    }

    @Test
    void testNetPayGreaterThanGrossPay() {
        payroll.setNetPay(new BigDecimal("6000.00")); // Greater than gross pay
        Set<ConstraintViolation<Payroll>> violations = validator.validate(payroll);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Net pay must be less than or equal to gross pay")));
    }

    @Test
    void testDeductionsGreaterThanGrossPay() {
        payroll.setDeductions(new BigDecimal("6000.00")); // Greater than gross pay
        Set<ConstraintViolation<Payroll>> violations = validator.validate(payroll);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Deductions must be less than or equal to gross pay")));
    }

    @Test
    void testPayPeriodFormatting() {
        payroll.setMonth(3);
        payroll.setYear(2024);
        assertEquals("03/2024", payroll.getPayPeriod());
    }

    @Test
    void testTaxAmountCalculation() {
        payroll.setGrossPay(new BigDecimal("5000.00"));
        payroll.setNetPay(new BigDecimal("4000.00"));
        payroll.setDeductions(new BigDecimal("500.00"));
        
        BigDecimal expectedTax = new BigDecimal("500.00"); // 5000 - 4000 - 500
        assertEquals(expectedTax, payroll.getTaxAmount());
    }

    @Test
    void testConstructorWithParameters() {
        Payroll newPayroll = new Payroll(user, 6, 2024, 
                                         new BigDecimal("6000.00"), 
                                         new BigDecimal("4800.00"), 
                                         new BigDecimal("600.00"));
        assertEquals(user, newPayroll.getUser());
        assertEquals(Integer.valueOf(6), newPayroll.getMonth());
        assertEquals(Integer.valueOf(2024), newPayroll.getYear());
        assertEquals(new BigDecimal("6000.00"), newPayroll.getGrossPay());
    }

    @Test
    void testEntityRelationships() {
        // Test user relationship
        assertNotNull(payroll.getUser());
        assertEquals(user, payroll.getUser());
    }

    @Test
    void testAuditFields() {
        // Test that audit fields are properly configured
        assertNull(payroll.getCreatedAt()); // Will be set by @CreationTimestamp
    }

    @Test
    void testCustomValidationWithValidAmounts() {
        payroll.setGrossPay(new BigDecimal("5000.00"));
        payroll.setNetPay(new BigDecimal("4000.00"));
        payroll.setDeductions(new BigDecimal("500.00"));
        
        assertTrue(payroll.isNetPayValid());
        assertTrue(payroll.isDeductionsValid());
    }

    @Test
    void testCustomValidationWithNullAmounts() {
        payroll.setGrossPay(null);
        payroll.setNetPay(null);
        payroll.setDeductions(null);
        
        assertTrue(payroll.isNetPayValid()); // Should be valid when null
        assertTrue(payroll.isDeductionsValid()); // Should be valid when null
    }

    @Test
    void testToString() {
        payroll.setId(1L);
        String toString = payroll.toString();
        assertTrue(toString.contains("Payroll{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("month=1"));
        assertTrue(toString.contains("year=2024"));
        assertTrue(toString.contains("user=John Doe"));
    }
}
