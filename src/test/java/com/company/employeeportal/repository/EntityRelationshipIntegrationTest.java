package com.company.employeeportal.repository;

import com.company.employeeportal.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for entity relationships and cascading operations.
 * Tests the complete entity relationship graph and data integrity.
 */
@DataJpaTest
class EntityRelationshipIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    private User employee;
    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        // Create test users with different roles
        employee = new User("John Employee", "john@array.world", "password123", Role.EMPLOYEE);
        employee.setDepartment("IT");
        employee.setPosition("Developer");
        employee.setJoinDate(LocalDate.now().minusYears(1));

        manager = new User("Jane Manager", "jane@array.world", "password456", Role.MANAGER);
        manager.setDepartment("IT");
        manager.setPosition("Team Lead");
        manager.setJoinDate(LocalDate.now().minusYears(2));

        admin = new User("Admin User", "admin@array.world", "password789", Role.ADMIN);
        admin.setDepartment("HR");
        admin.setPosition("System Administrator");
        admin.setJoinDate(LocalDate.now().minusYears(3));

        // Persist users
        employee = entityManager.persistAndFlush(employee);
        manager = entityManager.persistAndFlush(manager);
        admin = entityManager.persistAndFlush(admin);
    }

    @Test
    void testUserLeaveRelationship() {
        // Create leave request for employee
        Leave leave = new Leave(employee, LeaveType.ANNUAL, 
                               LocalDate.now().plusDays(10), 
                               LocalDate.now().plusDays(12), 
                               "Vacation");
        leave = entityManager.persistAndFlush(leave);

        // Test bidirectional relationship
        List<Leave> employeeLeaves = leaveRepository.findByUser(employee);
        assertEquals(1, employeeLeaves.size());
        assertEquals(leave.getId(), employeeLeaves.get(0).getId());

        // Test leave approval relationship
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(manager);
        leave.setManagerComments("Approved for vacation");
        entityManager.persistAndFlush(leave);

        // Verify approval relationship
        List<Leave> managerApprovedLeaves = leaveRepository.findByApprovedBy(manager);
        assertEquals(1, managerApprovedLeaves.size());
        assertEquals(leave.getId(), managerApprovedLeaves.get(0).getId());
    }

    @Test
    void testUserPayrollRelationship() {
        // Create payroll records for employee
        Payroll payroll1 = new Payroll(employee, 1, 2024, 
                                      new BigDecimal("5000.00"), 
                                      new BigDecimal("4000.00"), 
                                      new BigDecimal("1000.00"));
        payroll1.setDeductionDetails("Tax: 800, Insurance: 200");

        Payroll payroll2 = new Payroll(employee, 2, 2024, 
                                      new BigDecimal("5200.00"), 
                                      new BigDecimal("4100.00"), 
                                      new BigDecimal("1100.00"));
        payroll2.setDeductionDetails("Tax: 900, Insurance: 200");

        entityManager.persistAndFlush(payroll1);
        entityManager.persistAndFlush(payroll2);

        // Test relationship
        List<Payroll> employeePayrolls = payrollRepository.findByUser(employee);
        assertEquals(2, employeePayrolls.size());

        // Test unique constraint
        assertTrue(payrollRepository.existsByUserAndMonthAndYear(employee, 1, 2024));
        assertTrue(payrollRepository.existsByUserAndMonthAndYear(employee, 2, 2024));
        assertFalse(payrollRepository.existsByUserAndMonthAndYear(employee, 3, 2024));
    }

    @Test
    void testUserAnnouncementRelationship() {
        // Create announcements by admin
        Announcement announcement1 = new Announcement("Company Policy Update", 
                                                     "New remote work policy effective immediately.", 
                                                     admin);
        Announcement announcement2 = new Announcement("Holiday Schedule", 
                                                     "Please note the upcoming holiday dates.", 
                                                     admin);

        entityManager.persistAndFlush(announcement1);
        entityManager.persistAndFlush(announcement2);

        // Test relationship
        List<Announcement> adminAnnouncements = announcementRepository.findByCreatedBy(admin);
        assertEquals(2, adminAnnouncements.size());

        // Test active announcements
        List<Announcement> activeAnnouncements = announcementRepository.findByCreatedByAndActiveTrue(admin);
        assertEquals(2, activeAnnouncements.size());

        // Deactivate one announcement
        announcement1.deactivate();
        entityManager.persistAndFlush(announcement1);

        activeAnnouncements = announcementRepository.findByCreatedByAndActiveTrue(admin);
        assertEquals(1, activeAnnouncements.size());
        assertEquals("Holiday Schedule", activeAnnouncements.get(0).getTitle());
    }

    @Test
    void testComplexEntityRelationships() {
        // Create a complete scenario with all relationships
        
        // 1. Employee requests leave
        Leave leave = new Leave(employee, LeaveType.SICK, 
                               LocalDate.now().plusDays(5), 
                               LocalDate.now().plusDays(7), 
                               "Medical appointment");
        leave = entityManager.persistAndFlush(leave);

        // 2. Manager approves leave
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(manager);
        leave.setManagerComments("Approved for medical reasons");
        entityManager.persistAndFlush(leave);

        // 3. Create payroll for employee
        Payroll payroll = new Payroll(employee, 3, 2024, 
                                     new BigDecimal("5500.00"), 
                                     new BigDecimal("4300.00"), 
                                     new BigDecimal("1200.00"));
        entityManager.persistAndFlush(payroll);

        // 4. Admin creates announcement
        Announcement announcement = new Announcement("Sick Leave Policy", 
                                                   "Updated sick leave policy for all employees.", 
                                                   admin);
        entityManager.persistAndFlush(announcement);

        // Verify all relationships
        assertEquals(1, leaveRepository.findByUser(employee).size());
        assertEquals(1, leaveRepository.findByApprovedBy(manager).size());
        assertEquals(1, payrollRepository.findByUser(employee).size());
        assertEquals(1, announcementRepository.findByCreatedBy(admin).size());

        // Test cross-entity queries
        List<Leave> itDepartmentLeaves = leaveRepository.findByUserDepartment("IT");
        assertEquals(1, itDepartmentLeaves.size());

        List<User> managersAndAdmins = userRepository.findManagersAndAdmins();
        assertEquals(2, managersAndAdmins.size()); // Manager and Admin
    }

    @Test
    void testEntityValidationConstraints() {
        // Test User validation
        User invalidUser = new User();
        invalidUser.setName(""); // Invalid - blank name
        invalidUser.setEmail("invalid-email"); // Invalid - bad email format
        invalidUser.setPassword("123"); // Invalid - too short

        // This would fail validation if we tried to persist
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(invalidUser);
        });

        // Test Leave validation
        Leave invalidLeave = new Leave();
        invalidLeave.setUser(employee);
        invalidLeave.setLeaveType(LeaveType.ANNUAL);
        invalidLeave.setStartDate(LocalDate.now().plusDays(5));
        invalidLeave.setEndDate(LocalDate.now().plusDays(3)); // Invalid - end before start
        invalidLeave.setReason("Test");

        // This would fail validation
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(invalidLeave);
        });
    }

    @Test
    void testAuditFieldsPopulation() {
        // Create and persist entities to test audit fields
        User testUser = new User("Test User", "test@array.world", "password123", Role.EMPLOYEE);
        testUser = entityManager.persistAndFlush(testUser);

        // Audit fields should be populated after persistence
        assertNotNull(testUser.getCreatedAt());
        assertNotNull(testUser.getUpdatedAt());

        Leave testLeave = new Leave(testUser, LeaveType.PERSONAL, 
                                   LocalDate.now().plusDays(1), 
                                   LocalDate.now().plusDays(2), 
                                   "Personal matter");
        testLeave = entityManager.persistAndFlush(testLeave);

        assertNotNull(testLeave.getCreatedAt());
        assertNotNull(testLeave.getUpdatedAt());

        Payroll testPayroll = new Payroll(testUser, 4, 2024, 
                                         new BigDecimal("6000.00"), 
                                         new BigDecimal("4800.00"), 
                                         new BigDecimal("1200.00"));
        testPayroll = entityManager.persistAndFlush(testPayroll);

        assertNotNull(testPayroll.getCreatedAt());

        Announcement testAnnouncement = new Announcement("Test Announcement", 
                                                        "Test content", 
                                                        admin);
        testAnnouncement = entityManager.persistAndFlush(testAnnouncement);

        assertNotNull(testAnnouncement.getCreatedAt());
        assertNotNull(testAnnouncement.getUpdatedAt());
    }

    @Test
    void testRepositoryCustomQueries() {
        // Set up test data
        Leave pendingLeave = new Leave(employee, LeaveType.ANNUAL, 
                                      LocalDate.now().plusDays(15), 
                                      LocalDate.now().plusDays(17), 
                                      "Vacation");
        entityManager.persistAndFlush(pendingLeave);

        Payroll currentPayroll = new Payroll(employee, 5, 2024, 
                                            new BigDecimal("5800.00"), 
                                            new BigDecimal("4600.00"), 
                                            new BigDecimal("1200.00"));
        entityManager.persistAndFlush(currentPayroll);

        Announcement recentAnnouncement = new Announcement("Recent News", 
                                                          "Important company update", 
                                                          admin);
        entityManager.persistAndFlush(recentAnnouncement);

        // Test custom queries
        assertEquals(1, leaveRepository.countByStatus(LeaveStatus.PENDING));
        assertEquals(1, payrollRepository.countByUser(employee));
        assertEquals(1, announcementRepository.countByActiveTrue());

        // Test complex queries
        List<Leave> overlappingLeaves = leaveRepository.findOverlappingApprovedLeaves(
            employee, LocalDate.now().plusDays(14), LocalDate.now().plusDays(18));
        assertEquals(0, overlappingLeaves.size()); // No approved leaves yet

        // Test calculation queries
        assertTrue(payrollRepository.calculateTotalGrossPayForUserAndYear(employee, 2024).isPresent());
        assertTrue(payrollRepository.calculateTotalNetPayForUserAndYear(employee, 2024).isPresent());
    }
}
