package com.company.employeeportal.controller;

import com.company.employeeportal.exception.PayrollAccessException;
import com.company.employeeportal.model.Payroll;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.service.PayrollService;
import com.company.employeeportal.service.PdfGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PayrollController.
 */
@WebMvcTest(PayrollController.class)
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayrollService payrollService;

    @MockBean
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testEmployee;
    private Payroll testPayroll1;
    private Payroll testPayroll2;
    @
BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setName("John Doe");
        testEmployee.setEmail("john.doe@array.world");
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setDepartment("IT");
        testEmployee.setPosition("Developer");
        testEmployee.setJoinDate(LocalDate.of(2023, 1, 15));

        testPayroll1 = new Payroll();
        testPayroll1.setId(1L);
        testPayroll1.setUser(testEmployee);
        testPayroll1.setMonth(10);
        testPayroll1.setYear(2024);
        testPayroll1.setGrossPay(new BigDecimal("5000.00"));
        testPayroll1.setNetPay(new BigDecimal("4000.00"));
        testPayroll1.setDeductions(new BigDecimal("500.00"));
        testPayroll1.setCreatedAt(LocalDateTime.now());

        testPayroll2 = new Payroll();
        testPayroll2.setId(2L);
        testPayroll2.setUser(testEmployee);
        testPayroll2.setMonth(9);
        testPayroll2.setYear(2024);
        testPayroll2.setGrossPay(new BigDecimal("5000.00"));
        testPayroll2.setNetPay(new BigDecimal("4100.00"));
        testPayroll2.setDeductions(new BigDecimal("400.00"));
        testPayroll2.setCreatedAt(LocalDateTime.now().minusMonths(1));
    }

    @Test
    @WithMockUser(username = "john.doe@array.world", roles = "EMPLOYEE")
    void getMyPayslips_ShouldReturnPayslips_WhenAuthenticated() throws Exception {
        // Arrange
        List<Payroll> payslips = Arrays.asList(testPayroll1, testPayroll2);
        when(payrollService.getMyPayslips("john.doe@array.world")).thenReturn(payslips);

        // Act & Assert
        mockMvc.perform(get("/api/payroll/my-payslips")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].month").value(10))
                .andExpect(jsonPath("$[0].year").value(2024))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @WithMockUser(username = "john.doe@array.world", roles = "EMPLOYEE")
    void getMyPayslipsPaginated_ShouldReturnPaginatedResults() throws Exception {
        // Arrange
        List<Payroll> payslips = Arrays.asList(testPayroll1, testPayroll2);
        Page<Payroll> payrollPage = new PageImpl<>(payslips);
        when(payrollService.getMyPayslips(eq("john.doe@array.world"), eq(0), eq(10)))
                .thenReturn(payrollPage);

        // Act & Assert
        mockMvc.perform(get("/api/payroll/my-payslips/paginated")
                .param("page", "0")
                .param("size", "10")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
    
    @Test
    @WithMockUser(username = "john.doe@array.world", roles = "EMPLOYEE")
    void getPayslipById_ShouldReturnPayslip_WhenAuthorized() throws Exception {
        // Arrange
        when(payrollService.getPayslipById(1L, "john.doe@array.world")).thenReturn(testPayroll1);

        // Act & Assert
        mockMvc.perform(get("/api/payroll/payslip/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.year").value(2024));
    }

    @Test
    @WithMockUser(username = "john.doe@array.world", roles = "EMPLOYEE")
    void getPayslipById_ShouldReturnForbidden_WhenAccessDenied() throws Exception {
        // Arrange
        when(payrollService.getPayslipById(1L, "john.doe@array.world"))
                .thenThrow(new PayrollAccessException("Access denied"));

        // Act & Assert
        mockMvc.perform(get("/api/payroll/payslip/1")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "john.doe@array.world", roles = "EMPLOYEE")
    void getPayslipsByYear_ShouldReturnPayslipsForYear() throws Exception {
        // Arrange
        List<Payroll> payslips = Arrays.asList(testPayroll1, testPayroll2);
        when(payrollService.getPayslipsByYear("john.doe@array.world", 2024)).thenReturn(payslips);

        // Act & Assert
        mockMvc.perform(get("/api/payroll/my-payslips/year/2024")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "john.doe@array.world", roles = "EMPLOYEE")
    void downloadPayslipPdf_ShouldReturnPdf_WhenAuthorized() throws Exception {
        // Arrange
        byte[] pdfBytes = "PDF content".getBytes();
        when(payrollService.getPayslipById(1L, "john.doe@array.world")).thenReturn(testPayroll1);
        when(pdfGenerationService.generatePayslipPdf(testPayroll1)).thenReturn(pdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/payroll/payslip/1/download")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", 
                    "form-data; name=\"attachment\"; filename=\"payslip_John_Doe_10_2024.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void getMyPayslips_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/payroll/my-payslips"))
                .andExpect(status().isUnauthorized());
    }
}
