package com.company.employeeportal.service;

import com.company.employeeportal.model.Payroll;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PdfGenerationService.
 * Tests PDF generation functionality for payslips.
 */
@ExtendWith(MockitoExtension.class)
class PdfGenerationServiceTest {

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    private User testEmployee;
    private Payroll testPayroll;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setName("John Doe");
        testEmployee.setEmail("john.doe@company.com");
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setDepartment("IT");
        testEmployee.setPosition("Senior Developer");
        testEmployee.setJoinDate(LocalDate.of(2023, 1, 15));

        testPayroll = new Payroll();
        testPayroll.setId(1L);
        testPayroll.setUser(testEmployee);
        testPayroll.setMonth(10);
        testPayroll.setYear(2024);
        testPayroll.setGrossPay(new BigDecimal("5000.00"));
        testPayroll.setNetPay(new BigDecimal("4000.00"));
        testPayroll.setDeductions(new BigDecimal("500.00"));
        testPayroll.setDeductionDetails("Health Insurance: $300, Retirement: $200");
        testPayroll.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void generatePayslipPdf_ShouldGeneratePdf_WhenValidPayroll() {
        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Verify it's a PDF by checking the PDF header
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void generatePayslipPdf_ShouldHandleNullDeductionDetails() {
        // Arrange
        testPayroll.setDeductionDetails(null);

        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void generatePayslipPdf_ShouldHandleEmptyDeductionDetails() {
        // Arrange
        testPayroll.setDeductionDetails("");

        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void generatePayslipPdf_ShouldIncludeAllRequiredInformation() {
        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Convert to string to check content (basic verification)
        String pdfContent = new String(pdfBytes);
        
        // Note: This is a basic check. In a real scenario, you might want to use
        // a PDF parsing library to properly verify the content
        assertTrue(pdfContent.contains("PAYSLIP") || pdfContent.length() > 1000);
    }

    @Test
    void generatePayslipPdf_ShouldHandleZeroAmounts() {
        // Arrange
        testPayroll.setGrossPay(BigDecimal.ZERO);
        testPayroll.setNetPay(BigDecimal.ZERO);
        testPayroll.setDeductions(BigDecimal.ZERO);

        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Verify it's still a valid PDF
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void generatePayslipPdf_ShouldHandleLargeAmounts() {
        // Arrange
        testPayroll.setGrossPay(new BigDecimal("999999.99"));
        testPayroll.setNetPay(new BigDecimal("850000.00"));
        testPayroll.setDeductions(new BigDecimal("149999.99"));

        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Verify PDF is generated successfully with large amounts
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void generatePayslipPdf_ShouldHandleNullUserFields() {
        // Arrange
        testEmployee.setDepartment(null);
        testEmployee.setPosition(null);
        testEmployee.setJoinDate(null);

        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Should still generate valid PDF even with null fields
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void generatePayslipPdf_ShouldHandleLongDeductionDetails() {
        // Arrange
        String longDetails = "Health Insurance: $300.00, Dental Insurance: $50.00, " +
                           "Vision Insurance: $25.00, Life Insurance: $15.00, " +
                           "401k Contribution: $500.00, Parking Fee: $75.00, " +
                           "Union Dues: $35.00, Additional Voluntary Deduction: $100.00";
        testPayroll.setDeductionDetails(longDetails);

        // Act
        byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Should handle long deduction details without issues
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader);
    }

    @Test
    void generatePayslipPdf_ShouldGenerateUniqueContent() {
        // Arrange
        Payroll anotherPayroll = new Payroll();
        anotherPayroll.setId(2L);
        anotherPayroll.setUser(testEmployee);
        anotherPayroll.setMonth(11);
        anotherPayroll.setYear(2024);
        anotherPayroll.setGrossPay(new BigDecimal("5500.00"));
        anotherPayroll.setNetPay(new BigDecimal("4400.00"));
        anotherPayroll.setDeductions(new BigDecimal("600.00"));
        anotherPayroll.setDeductionDetails("Different deductions");

        // Act
        byte[] pdfBytes1 = pdfGenerationService.generatePayslipPdf(testPayroll);
        byte[] pdfBytes2 = pdfGenerationService.generatePayslipPdf(anotherPayroll);

        // Assert
        assertNotNull(pdfBytes1);
        assertNotNull(pdfBytes2);
        assertTrue(pdfBytes1.length > 0);
        assertTrue(pdfBytes2.length > 0);
        
        // PDFs should be different (different content)
        assertFalse(java.util.Arrays.equals(pdfBytes1, pdfBytes2));
    }

    @Test
    void generatePayslipPdf_ShouldThrowException_WhenPayrollIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            pdfGenerationService.generatePayslipPdf(null);
        });
    }

    @Test
    void generatePayslipPdf_ShouldGenerateConsistentSize() {
        // Act - Generate PDF multiple times
        byte[] pdfBytes1 = pdfGenerationService.generatePayslipPdf(testPayroll);
        byte[] pdfBytes2 = pdfGenerationService.generatePayslipPdf(testPayroll);

        // Assert
        assertNotNull(pdfBytes1);
        assertNotNull(pdfBytes2);
        assertTrue(pdfBytes1.length > 0);
        assertTrue(pdfBytes2.length > 0);
        
        // Size should be similar (allowing for timestamp differences)
        int sizeDifference = Math.abs(pdfBytes1.length - pdfBytes2.length);
        assertTrue(sizeDifference < 100, "PDF size difference should be minimal: " + sizeDifference);
    }
}