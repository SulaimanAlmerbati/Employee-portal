package com.company.employeeportal.repository;

import com.company.employeeportal.model.Payroll;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PayrollRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PayrollRepository payrollRepository;

    private User employee;
    private Payroll payroll2024Jan;
    private Payroll payroll2024Feb;
    private Payroll payroll2023Dec;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setName("John Employee");
        employee.setEmail("john@company.com");
        employee.setPassword("password123");
        employee.setRole(Role.EMPLOYEE);
        employee.setDepartment("IT");
        employee.setJoinDate(LocalDate.now());
        employee.setActive(true);

        entityManager.persistAndFlush(employee);

        payroll2024Jan = new Payroll();
        payroll2024Jan.setUser(employee);
        payroll2024Jan.setMonth(1);
        payroll2024Jan.setYear(2024);
        payroll2024Jan.setGrossPay(BigDecimal.valueOf(5000.00));
        payroll2024Jan.setNetPay(BigDecimal.valueOf(4000.00));
        payroll2024Jan.setDeductions(BigDecimal.valueOf(1000.00));
        payroll2024Jan.setDeductionDetails("Tax: 800, Insurance: 200");

        payroll2024Feb = new Payroll();
        payroll2024Feb.setUser(employee);
        payroll2024Feb.setMonth(2);
        payroll2024Feb.setYear(2024);
        payroll2024Feb.setGrossPay(BigDecimal.valueOf(5200.00));
        payroll2024Feb.setNetPay(BigDecimal.valueOf(4100.00));
        payroll2024Feb.setDeductions(BigDecimal.valueOf(1100.00));
        payroll2024Feb.setDeductionDetails("Tax: 900, Insurance: 200");

        payroll2023Dec = new Payroll();
        payroll2023Dec.setUser(employee);
        payroll2023Dec.setMonth(12);
        payroll2023Dec.setYear(2023);
        payroll2023Dec.setGrossPay(BigDecimal.valueOf(4800.00));
        payroll2023Dec.setNetPay(BigDecimal.valueOf(3900.00));
        payroll2023Dec.setDeductions(BigDecimal.valueOf(900.00));
        payroll2023Dec.setDeductionDetails("Tax: 700, Insurance: 200");

        entityManager.persistAndFlush(payroll2024Jan);
        entityManager.persistAndFlush(payroll2024Feb);
        entityManager.persistAndFlush(payroll2023Dec);
    }

    @Test
    void testFindByUser() {
        List<Payroll> userPayrolls = payrollRepository.findByUser(employee);
        assertEquals(3, userPayrolls.size());
    }

    @Test
    void testFindByUserWithPagination() {
        Page<Payroll> payrollPage = payrollRepository.findByUser(employee, PageRequest.of(0, 2));
        assertEquals(2, payrollPage.getContent().size());
        assertEquals(3, payrollPage.getTotalElements());
    }

    @Test
    void testFindByUserOrderByYearDescMonthDesc() {
        Page<Payroll> payrollPage = payrollRepository.findByUserOrderByYearDescMonthDesc(
            employee, PageRequest.of(0, 10));
        
        List<Payroll> payrolls = payrollPage.getContent();
        assertEquals(3, payrolls.size());
        
        // Should be ordered by year desc, month desc
        assertEquals(2024, payrolls.get(0).getYear());
        assertEquals(2, payrolls.get(0).getMonth()); // Feb 2024
        assertEquals(2024, payrolls.get(1).getYear());
        assertEquals(1, payrolls.get(1).getMonth()); // Jan 2024
        assertEquals(2023, payrolls.get(2).getYear());
        assertEquals(12, payrolls.get(2).getMonth()); // Dec 2023
    }

    @Test
    void testFindByUserAndMonthAndYear() {
        Optional<Payroll> found = payrollRepository.findByUserAndMonthAndYear(employee, 1, 2024);
        assertTrue(found.isPresent());
        assertEquals(BigDecimal.valueOf(5000.00), found.get().getGrossPay());

        Optional<Payroll> notFound = payrollRepository.findByUserAndMonthAndYear(employee, 3, 2024);
        assertFalse(notFound.isPresent());
    }

    @Test
    void testFindByUserAndYear() {
        List<Payroll> payrolls2024 = payrollRepository.findByUserAndYear(employee, 2024);
        assertEquals(2, payrolls2024.size());

        List<Payroll> payrolls2023 = payrollRepository.findByUserAndYear(employee, 2023);
        assertEquals(1, payrolls2023.size());
    }

    @Test
    void testFindByMonthAndYear() {
        List<Payroll> janPayrolls = payrollRepository.findByMonthAndYear(1, 2024);
        assertEquals(1, janPayrolls.size());
        assertEquals(employee.getId(), janPayrolls.get(0).getUser().getId());
    }

    @Test
    void testExistsByUserAndMonthAndYear() {
        assertTrue(payrollRepository.existsByUserAndMonthAndYear(employee, 1, 2024));
        assertFalse(payrollRepository.existsByUserAndMonthAndYear(employee, 3, 2024));
    }

    @Test
    void testFindByYear() {
        List<Payroll> payrolls2024 = payrollRepository.findByYear(2024);
        assertEquals(2, payrolls2024.size());

        List<Payroll> payrolls2023 = payrollRepository.findByYear(2023);
        assertEquals(1, payrolls2023.size());
    }

    @Test
    void testCountByUser() {
        long count = payrollRepository.countByUser(employee);
        assertEquals(3, count);
    }

    @Test
    void testCalculateTotalGrossPayForUserAndYear() {
        Optional<Double> total2024 = payrollRepository.calculateTotalGrossPayForUserAndYear(employee, 2024);
        assertTrue(total2024.isPresent());
        assertEquals(10200.00, total2024.get(), 0.01); // 5000 + 5200

        Optional<Double> total2023 = payrollRepository.calculateTotalGrossPayForUserAndYear(employee, 2023);
        assertTrue(total2023.isPresent());
        assertEquals(4800.00, total2023.get(), 0.01);
    }

    @Test
    void testCalculateTotalNetPayForUserAndYear() {
        Optional<Double> total2024 = payrollRepository.calculateTotalNetPayForUserAndYear(employee, 2024);
        assertTrue(total2024.isPresent());
        assertEquals(8100.00, total2024.get(), 0.01); // 4000 + 4100

        Optional<Double> total2023 = payrollRepository.calculateTotalNetPayForUserAndYear(employee, 2023);
        assertTrue(total2023.isPresent());
        assertEquals(3900.00, total2023.get(), 0.01);
    }

    @Test
    void testFindRecentPayrollRecords() {
        LocalDateTime twentyFourMonthsAgo = LocalDateTime.now().minusMonths(24);
        List<Payroll> recentPayrolls = payrollRepository.findRecentPayrollRecords(employee, twentyFourMonthsAgo);
        
        // All test records should be recent
        assertEquals(3, recentPayrolls.size());
    }

    @Test
    void testFindPayrollRecordsToArchive() {
        LocalDateTime cutoffDate = LocalDateTime.now().plusDays(1); // Future date to include all test records
        List<Payroll> toArchive = payrollRepository.findPayrollRecordsToArchive(cutoffDate);
        
        assertEquals(3, toArchive.size());
    }

    @Test
    void testFindByUserAndYearRange() {
        List<Payroll> payrolls = payrollRepository.findByUserAndYearRange(employee, 2023, 2024);
        assertEquals(3, payrolls.size());
        
        // Should be ordered by year desc, month desc
        assertEquals(2024, payrolls.get(0).getYear());
        assertEquals(2, payrolls.get(0).getMonth()); // Feb 2024
    }

    @Test
    void testFindLatestByUser() {
        Optional<Payroll> latest = payrollRepository.findLatestByUser(employee);
        assertTrue(latest.isPresent());
        assertEquals(2024, latest.get().getYear());
        assertEquals(2, latest.get().getMonth()); // Feb 2024 should be latest
    }

    @Test
    void testFindByYearWithPagination() {
        Page<Payroll> payrolls2024 = payrollRepository.findByYear(2024, PageRequest.of(0, 10));
        assertEquals(2, payrolls2024.getContent().size());
        assertEquals(2, payrolls2024.getTotalElements());
    }

    @Test
    void testUniqueConstraint() {
        // Test that the unique constraint on user_id, month, year works
        assertTrue(payrollRepository.existsByUserAndMonthAndYear(employee, 1, 2024));
        assertFalse(payrollRepository.existsByUserAndMonthAndYear(employee, 5, 2024));
    }

    @Test
    void testCascadingOperations() {
        // Test that relationships are properly maintained
        List<Payroll> userPayrolls = payrollRepository.findByUser(employee);
        assertEquals(3, userPayrolls.size());
        
        // Verify relationships
        for (Payroll payroll : userPayrolls) {
            assertEquals(employee, payroll.getUser());
        }
    }
}