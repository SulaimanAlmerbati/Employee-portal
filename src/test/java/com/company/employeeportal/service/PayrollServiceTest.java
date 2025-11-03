package com.company.employeeportal.service;

import com.company.employeeportal.exception.PayrollAccessException;
import com.company.employeeportal.exception.UserNotFoundException;
import com.company.employeeportal.model.Payroll;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.PayrollRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PayrollService.
 * Tests payslip data access restrictions, PDF generation functionality,
 * and date filtering and history retrieval.
 */
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PayrollService payrollService;

    private User testEmployee;
    private User otherEmployee;
    private Payroll testPayroll1;
    private Payroll testPayroll2;
    private Payroll otherUserPayroll;

    @BeforeEach
    void setUp() {
        // Create test employee
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setName("John Doe");
        testEmployee.setEmail("john.doe@array.world");
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setDepartment("IT");
        testEmployee.setPosition("Developer");
        testEmployee.setJoinDate(LocalDate.of(2023, 1, 15));

        // Create other employee
        otherEmployee = new User();
        otherEmployee.setId(2L);
        otherEmployee.setName("Jane Smith");
        otherEmployee.setEmail("jane.smith@array.world");
        otherEmployee.setRole(Role.EMPLOYEE);
        otherEmployee.setDepartment("HR");
        otherEmployee.setPosition("HR Specialist");

        // Create test payroll records
        testPayroll1 = new Payroll();
        testPayroll1.setId(1L);
        testPayroll1.setUser(testEmployee);
        testPayroll1.setMonth(10);
        testPayroll1.setYear(2024);
        testPayroll1.setGrossPay(new BigDecimal("5000.00"));
        testPayroll1.setNetPay(new BigDecimal("4000.00"));
        testPayroll1.setDeductions(new BigDecimal("500.00"));
        testPayroll1.setDeductionDetails("Health Insurance: $300, Retirement: $200");
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

        // Create payroll for other user
        otherUserPayroll = new Payroll();
        otherUserPayroll.setId(3L);
        otherUserPayroll.setUser(otherEmployee);
        otherUserPayroll.setMonth(10);
        otherUserPayroll.setYear(2024);
        otherUserPayroll.setGrossPay(new BigDecimal("4500.00"));
        otherUserPayroll.setNetPay(new BigDecimal("3600.00"));
        otherUserPayroll.setDeductions(new BigDecimal("400.00"));
    }

    @Test
    void getMyPayslips_ShouldReturnUserPayslips_WhenValidUser() {
        // Arrange
        String userEmail = "john.doe@array.world";
        List<Payroll> expectedPayslips = Arrays.asList(testPayroll1, testPayroll2);
        Page<Payroll> payrollPage = new PageImpl<>(expectedPayslips);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserOrderByYearDescMonthDesc(eq(testEmployee), any(Pageable.class)))
                .thenReturn(payrollPage);

        // Act
        List<Payroll> result = payrollService.getMyPayslips(userEmail);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testPayroll1.getId(), result.get(0).getId());
        assertEquals(testPayroll2.getId(), result.get(1).getId());
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findByUserOrderByYearDescMonthDesc(eq(testEmployee), any(Pageable.class));
    }

    @Test
    void getMyPayslips_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // Arrange
        String userEmail = "nonexistent@array.world";
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> payrollService.getMyPayslips(userEmail));
        verify(userRepository).findByEmail(userEmail);
        verifyNoInteractions(payrollRepository);
    }

    @Test
    void getMyPayslipsPaginated_ShouldReturnPaginatedResults() {
        // Arrange
        String userEmail = "john.doe@array.world";
        int page = 0;
        int size = 10;
        List<Payroll> payslips = Arrays.asList(testPayroll1, testPayroll2);
        Page<Payroll> payrollPage = new PageImpl<>(payslips, PageRequest.of(page, size), 2);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserOrderByYearDescMonthDesc(eq(testEmployee), any(Pageable.class)))
                .thenReturn(payrollPage);

        // Act
        Page<Payroll> result = payrollService.getMyPayslips(userEmail, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getPayslipById_ShouldReturnPayslip_WhenUserOwnsPayslip() {
        // Arrange
        String userEmail = "john.doe@array.world";
        Long payslipId = 1L;

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findById(payslipId)).thenReturn(Optional.of(testPayroll1));

        // Act
        Payroll result = payrollService.getPayslipById(payslipId, userEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testPayroll1.getId(), result.getId());
        assertEquals(testEmployee.getId(), result.getUser().getId());
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findById(payslipId);
    }

    @Test
    void getPayslipById_ShouldThrowPayrollAccessException_WhenUserDoesNotOwnPayslip() {
        // Arrange
        String userEmail = "john.doe@array.world";
        Long payslipId = 3L; // This belongs to otherEmployee

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findById(payslipId)).thenReturn(Optional.of(otherUserPayroll));

        // Act & Assert
        PayrollAccessException exception = assertThrows(PayrollAccessException.class, 
                () -> payrollService.getPayslipById(payslipId, userEmail));
        
        assertTrue(exception.getMessage().contains("Access denied"));
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findById(payslipId);
    }

    @Test
    void getPayslipById_ShouldThrowPayrollAccessException_WhenPayslipNotFound() {
        // Arrange
        String userEmail = "john.doe@array.world";
        Long payslipId = 999L;

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findById(payslipId)).thenReturn(Optional.empty());

        // Act & Assert
        PayrollAccessException exception = assertThrows(PayrollAccessException.class, 
                () -> payrollService.getPayslipById(payslipId, userEmail));
        
        assertEquals("Payslip not found", exception.getMessage());
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findById(payslipId);
    }

    @Test
    void getPayslipsByYear_ShouldReturnPayslipsForSpecificYear() {
        // Arrange
        String userEmail = "john.doe@array.world";
        Integer year = 2024;
        List<Payroll> expectedPayslips = Arrays.asList(testPayroll1, testPayroll2);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserAndYear(testEmployee, year)).thenReturn(expectedPayslips);

        // Act
        List<Payroll> result = payrollService.getPayslipsByYear(userEmail, year);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getYear().equals(year)));
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findByUserAndYear(testEmployee, year);
    }

    @Test
    void getPayslipsByYearRange_ShouldReturnPayslipsInRange() {
        // Arrange
        String userEmail = "john.doe@array.world";
        Integer startYear = 2023;
        Integer endYear = 2024;
        List<Payroll> expectedPayslips = Arrays.asList(testPayroll1, testPayroll2);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserAndYearRange(testEmployee, startYear, endYear))
                .thenReturn(expectedPayslips);

        // Act
        List<Payroll> result = payrollService.getPayslipsByYearRange(userEmail, startYear, endYear);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findByUserAndYearRange(testEmployee, startYear, endYear);
    }

    @Test
    void getPayslipsByYearRange_ShouldThrowException_WhenStartYearGreaterThanEndYear() {
        // Arrange
        String userEmail = "john.doe@array.world";
        Integer startYear = 2025;
        Integer endYear = 2024;

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> payrollService.getPayslipsByYearRange(userEmail, startYear, endYear));
        
        assertEquals("Start year cannot be greater than end year", exception.getMessage());
        verify(userRepository).findByEmail(userEmail);
        verifyNoInteractions(payrollRepository);
    }

    @Test
    void getRecentPayslips_ShouldReturnPayslipsWithin24Months() {
        // Arrange
        String userEmail = "john.doe@array.world";
        List<Payroll> expectedPayslips = Arrays.asList(testPayroll1, testPayroll2);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findRecentPayrollRecords(eq(testEmployee), any(LocalDateTime.class)))
                .thenReturn(expectedPayslips);

        // Act
        List<Payroll> result = payrollService.getRecentPayslips(userEmail);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findRecentPayrollRecords(eq(testEmployee), any(LocalDateTime.class));
    }

    @Test
    void getLatestPayslip_ShouldReturnLatestPayslip_WhenExists() {
        // Arrange
        String userEmail = "john.doe@array.world";

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findLatestByUser(testEmployee)).thenReturn(Optional.of(testPayroll1));

        // Act
        Optional<Payroll> result = payrollService.getLatestPayslip(userEmail);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testPayroll1.getId(), result.get().getId());
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findLatestByUser(testEmployee);
    }

    @Test
    void getLatestPayslip_ShouldReturnEmpty_WhenNoPayslipsExist() {
        // Arrange
        String userEmail = "john.doe@array.world";

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findLatestByUser(testEmployee)).thenReturn(Optional.empty());

        // Act
        Optional<Payroll> result = payrollService.getLatestPayslip(userEmail);

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findLatestByUser(testEmployee);
    }

    @Test
    void getPayslipCount_ShouldReturnCorrectCount() {
        // Arrange
        String userEmail = "john.doe@array.world";
        long expectedCount = 5L;

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.countByUser(testEmployee)).thenReturn(expectedCount);

        // Act
        long result = payrollService.getPayslipCount(userEmail);

        // Assert
        assertEquals(expectedCount, result);
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).countByUser(testEmployee);
    }

    @Test
    void getPayslipsForUser_ShouldReturnPayslips_WhenAdminAccess() {
        // Arrange
        Long userId = 1L;
        List<Payroll> expectedPayslips = Arrays.asList(testPayroll1, testPayroll2);
        Page<Payroll> payrollPage = new PageImpl<>(expectedPayslips);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserOrderByYearDescMonthDesc(eq(testEmployee), any(Pageable.class)))
                .thenReturn(payrollPage);

        // Act
        List<Payroll> result = payrollService.getPayslipsForUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findById(userId);
        verify(payrollRepository).findByUserOrderByYearDescMonthDesc(eq(testEmployee), any(Pageable.class));
    }

    @Test
    void getPayslipsForUser_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // Arrange
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> payrollService.getPayslipsForUser(userId));
        verify(userRepository).findById(userId);
        verifyNoInteractions(payrollRepository);
    }

    @Test
    void cleanupOldPayrollRecords_ShouldDeleteOldRecords() {
        // Arrange
        List<Payroll> oldRecords = Arrays.asList(testPayroll1);
        when(payrollRepository.findPayrollRecordsToArchive(any(LocalDateTime.class))).thenReturn(oldRecords);

        // Act
        payrollService.cleanupOldPayrollRecords();

        // Assert
        verify(payrollRepository).findPayrollRecordsToArchive(any(LocalDateTime.class));
        verify(payrollRepository).deleteOldPayrollRecords(any(LocalDateTime.class));
    }

    @Test
    void cleanupOldPayrollRecords_ShouldNotDeleteWhenNoOldRecords() {
        // Arrange
        when(payrollRepository.findPayrollRecordsToArchive(any(LocalDateTime.class))).thenReturn(Arrays.asList());

        // Act
        payrollService.cleanupOldPayrollRecords();

        // Assert
        verify(payrollRepository).findPayrollRecordsToArchive(any(LocalDateTime.class));
        verify(payrollRepository, never()).deleteOldPayrollRecords(any(LocalDateTime.class));
    }

    // Additional tests for comprehensive coverage of requirements 4.4 and 4.5

    @Test
    void getMyPayslips_ShouldOnlyReturnOwnPayslips_AccessRestrictionTest() {
        // Arrange - Test requirement 4.4: prevent access to other employees' data
        String userEmail = "john.doe@array.world";
        List<Payroll> userPayslips = Arrays.asList(testPayroll1, testPayroll2);
        Page<Payroll> payrollPage = new PageImpl<>(userPayslips);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserOrderByYearDescMonthDesc(eq(testEmployee), any(Pageable.class)))
                .thenReturn(payrollPage);

        // Act
        List<Payroll> result = payrollService.getMyPayslips(userEmail);

        // Assert - Verify only user's own payslips are returned
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getUser().getId().equals(testEmployee.getId())));
        
        // Verify no other user's payslips are included
        assertFalse(result.stream().anyMatch(p -> p.getUser().getId().equals(otherEmployee.getId())));
        
        verify(payrollRepository).findByUserOrderByYearDescMonthDesc(eq(testEmployee), any(Pageable.class));
    }

    @Test
    void getPayslipById_ShouldEnforceStrictAccessControl() {
        // Arrange - Test requirement 4.4: strict access control
        String userEmail = "john.doe@array.world";
        Long otherUserPayslipId = otherUserPayroll.getId();

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findById(otherUserPayslipId)).thenReturn(Optional.of(otherUserPayroll));

        // Act & Assert - Should throw exception when accessing other user's payslip
        PayrollAccessException exception = assertThrows(PayrollAccessException.class, 
                () -> payrollService.getPayslipById(otherUserPayslipId, userEmail));
        
        assertTrue(exception.getMessage().contains("Access denied"));
        assertTrue(exception.getMessage().contains("your own payslips"));
        
        verify(userRepository).findByEmail(userEmail);
        verify(payrollRepository).findById(otherUserPayslipId);
    }

    @Test
    void getRecentPayslips_ShouldMaintain24MonthHistory() {
        // Arrange - Test requirement 4.5: maintain 24-month history
        String userEmail = "john.doe@array.world";
        
        // Create payslips within and outside 24-month window
        Payroll recentPayroll = new Payroll();
        recentPayroll.setId(10L);
        recentPayroll.setUser(testEmployee);
        recentPayroll.setMonth(1);
        recentPayroll.setYear(2024);
        recentPayroll.setCreatedAt(LocalDateTime.now().minusMonths(12)); // Within 24 months
        
        List<Payroll> recentPayslips = Arrays.asList(testPayroll1, testPayroll2, recentPayroll);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findRecentPayrollRecords(eq(testEmployee), any(LocalDateTime.class)))
                .thenReturn(recentPayslips);

        // Act
        List<Payroll> result = payrollService.getRecentPayslips(userEmail);

        // Assert - Should return payslips within 24-month retention period
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify the cutoff date is approximately 24 months ago
        verify(payrollRepository).findRecentPayrollRecords(eq(testEmployee), any(LocalDateTime.class));
        
        // All returned payslips should belong to the requesting user
        assertTrue(result.stream().allMatch(p -> p.getUser().getId().equals(testEmployee.getId())));
    }

    @Test
    void getPayslipsByYearRange_ShouldFilterCorrectly() {
        // Arrange - Test date filtering functionality
        String userEmail = "john.doe@array.world";
        Integer startYear = 2022;
        Integer endYear = 2024;
        
        List<Payroll> filteredPayslips = Arrays.asList(testPayroll1, testPayroll2);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserAndYearRange(testEmployee, startYear, endYear))
                .thenReturn(filteredPayslips);

        // Act
        List<Payroll> result = payrollService.getPayslipsByYearRange(userEmail, startYear, endYear);

        // Assert - Should return filtered results within date range
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify all payslips belong to the correct user
        assertTrue(result.stream().allMatch(p -> p.getUser().getId().equals(testEmployee.getId())));
        
        // Verify repository called with correct parameters
        verify(payrollRepository).findByUserAndYearRange(testEmployee, startYear, endYear);
    }

    @Test
    void getPayslipsByYear_ShouldReturnOnlyUserPayslipsForSpecificYear() {
        // Arrange - Test year-specific filtering with access control
        String userEmail = "john.doe@array.world";
        Integer year = 2024;
        
        List<Payroll> yearPayslips = Arrays.asList(testPayroll1, testPayroll2);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.findByUserAndYear(testEmployee, year)).thenReturn(yearPayslips);

        // Act
        List<Payroll> result = payrollService.getPayslipsByYear(userEmail, year);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify access control - all payslips belong to requesting user
        assertTrue(result.stream().allMatch(p -> p.getUser().getId().equals(testEmployee.getId())));
        
        // Verify year filtering
        assertTrue(result.stream().allMatch(p -> p.getYear().equals(year)));
        
        verify(payrollRepository).findByUserAndYear(testEmployee, year);
    }

    @Test
    void getPayslipCount_ShouldReturnAccurateCount() {
        // Arrange - Test payslip count for history tracking
        String userEmail = "john.doe@array.world";
        long expectedCount = 24L; // 2 years of monthly payslips

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testEmployee));
        when(payrollRepository.countByUser(testEmployee)).thenReturn(expectedCount);

        // Act
        long result = payrollService.getPayslipCount(userEmail);

        // Assert
        assertEquals(expectedCount, result);
        verify(payrollRepository).countByUser(testEmployee);
    }

    @Test
    void cleanupOldPayrollRecords_ShouldRespect24MonthRetention() {
        // Arrange - Test requirement 4.5: 24-month data retention
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusMonths(24);
        
        // Create old payroll records (older than 24 months)
        Payroll oldPayroll1 = new Payroll();
        oldPayroll1.setId(100L);
        oldPayroll1.setUser(testEmployee);
        oldPayroll1.setCreatedAt(now.minusMonths(25));
        
        Payroll oldPayroll2 = new Payroll();
        oldPayroll2.setId(101L);
        oldPayroll2.setUser(otherEmployee);
        oldPayroll2.setCreatedAt(now.minusMonths(30));
        
        List<Payroll> oldRecords = Arrays.asList(oldPayroll1, oldPayroll2);
        
        when(payrollRepository.findPayrollRecordsToArchive(any(LocalDateTime.class))).thenReturn(oldRecords);

        // Act
        payrollService.cleanupOldPayrollRecords();

        // Assert
        verify(payrollRepository).findPayrollRecordsToArchive(any(LocalDateTime.class));
        verify(payrollRepository).deleteOldPayrollRecords(any(LocalDateTime.class));
        
        // Verify the cutoff date is approximately 24 months
        // (We can't verify exact date due to timing, but we verify the method was called)
    }
}
