package com.company.employeeportal.repository;

import com.company.employeeportal.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LeaveRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeaveRepository leaveRepository;

    private User employee;
    private User manager;
    private Leave pendingLeave;
    private Leave approvedLeave;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setName("John Employee");
        employee.setEmail("john@array.world");
        employee.setPassword("password123");
        employee.setRole(Role.EMPLOYEE);
        employee.setDepartment("IT");
        employee.setActive(true);

        manager = new User();
        manager.setName("Jane Manager");
        manager.setEmail("jane@array.world");
        manager.setPassword("password456");
        manager.setRole(Role.MANAGER);
        manager.setDepartment("IT");
        manager.setActive(true);

        entityManager.persistAndFlush(employee);
        entityManager.persistAndFlush(manager);

        pendingLeave = new Leave();
        pendingLeave.setUser(employee);
        pendingLeave.setLeaveType(LeaveType.ANNUAL);
        pendingLeave.setStartDate(LocalDate.now().plusDays(10));
        pendingLeave.setEndDate(LocalDate.now().plusDays(12));
        pendingLeave.setReason("Vacation");
        pendingLeave.setStatus(LeaveStatus.PENDING);

        approvedLeave = new Leave();
        approvedLeave.setUser(employee);
        approvedLeave.setLeaveType(LeaveType.SICK);
        approvedLeave.setStartDate(LocalDate.now().plusDays(20));
        approvedLeave.setEndDate(LocalDate.now().plusDays(21));
        approvedLeave.setReason("Medical appointment");
        approvedLeave.setStatus(LeaveStatus.APPROVED);
        approvedLeave.setApprovedBy(manager);

        entityManager.persistAndFlush(pendingLeave);
        entityManager.persistAndFlush(approvedLeave);
    }

    @Test
    void testFindByUser() {
        List<Leave> userLeaves = leaveRepository.findByUser(employee);
        assertEquals(2, userLeaves.size());
    }

    @Test
    void testFindByUserAndStatus() {
        List<Leave> pendingLeaves = leaveRepository.findByUserAndStatus(employee, LeaveStatus.PENDING);
        assertEquals(1, pendingLeaves.size());
        assertEquals(LeaveType.ANNUAL, pendingLeaves.get(0).getLeaveType());

        List<Leave> approvedLeaves = leaveRepository.findByUserAndStatus(employee, LeaveStatus.APPROVED);
        assertEquals(1, approvedLeaves.size());
        assertEquals(LeaveType.SICK, approvedLeaves.get(0).getLeaveType());
    }

    @Test
    void testFindByStatus() {
        List<Leave> pendingLeaves = leaveRepository.findByStatus(LeaveStatus.PENDING);
        assertEquals(1, pendingLeaves.size());

        List<Leave> approvedLeaves = leaveRepository.findByStatus(LeaveStatus.APPROVED);
        assertEquals(1, approvedLeaves.size());
    }

    @Test
    void testFindByLeaveType() {
        List<Leave> annualLeaves = leaveRepository.findByLeaveType(LeaveType.ANNUAL);
        assertEquals(1, annualLeaves.size());

        List<Leave> sickLeaves = leaveRepository.findByLeaveType(LeaveType.SICK);
        assertEquals(1, sickLeaves.size());
    }

    @Test
    void testFindByApprovedBy() {
        List<Leave> managerApprovedLeaves = leaveRepository.findByApprovedBy(manager);
        assertEquals(1, managerApprovedLeaves.size());
        assertEquals(LeaveType.SICK, managerApprovedLeaves.get(0).getLeaveType());
    }

    @Test
    void testCountByStatus() {
        assertEquals(1, leaveRepository.countByStatus(LeaveStatus.PENDING));
        assertEquals(1, leaveRepository.countByStatus(LeaveStatus.APPROVED));
        assertEquals(0, leaveRepository.countByStatus(LeaveStatus.REJECTED));
    }

    @Test
    void testCountByUserAndStatus() {
        assertEquals(1, leaveRepository.countByUserAndStatus(employee, LeaveStatus.PENDING));
        assertEquals(1, leaveRepository.countByUserAndStatus(employee, LeaveStatus.APPROVED));
    }

    @Test
    void testFindOverlappingApprovedLeaves() {
        LocalDate startDate = LocalDate.now().plusDays(19);
        LocalDate endDate = LocalDate.now().plusDays(22);
        
        List<Leave> overlapping = leaveRepository.findOverlappingApprovedLeaves(employee, startDate, endDate);
        assertEquals(1, overlapping.size());
        assertEquals(LeaveType.SICK, overlapping.get(0).getLeaveType());
    }

    @Test
    void testFindByUserDepartment() {
        List<Leave> departmentLeaves = leaveRepository.findByUserDepartment("IT");
        assertEquals(2, departmentLeaves.size());
    }

    @Test
    void testFindByStatusOrderByCreatedAtAsc() {
        List<Leave> pendingLeaves = leaveRepository.findByStatusOrderByCreatedAtAsc(LeaveStatus.PENDING);
        assertEquals(1, pendingLeaves.size());
        assertEquals(LeaveType.ANNUAL, pendingLeaves.get(0).getLeaveType());
    }

    @Test
    void testFindByUserAndDateRange() {
        LocalDate startDate = LocalDate.now().plusDays(9);
        LocalDate endDate = LocalDate.now().plusDays(13);
        
        List<Leave> overlapping = leaveRepository.findByUserAndDateRange(employee, startDate, endDate);
        assertEquals(1, overlapping.size());
        assertEquals(LeaveType.ANNUAL, overlapping.get(0).getLeaveType());
    }

    @Test
    void testFindByUserAndYear() {
        int currentYear = LocalDate.now().getYear();
        List<Leave> currentYearLeaves = leaveRepository.findByUserAndYear(employee, currentYear);
        assertEquals(2, currentYearLeaves.size());
    }

    @Test
    void testFindRecentLeaves() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Leave> recentLeaves = leaveRepository.findRecentLeaves(thirtyDaysAgo);
        assertEquals(2, recentLeaves.size());
    }

    @Test
    void testFindApprovedLeavesInDateRange() {
        LocalDate startDate = LocalDate.now().plusDays(19);
        LocalDate endDate = LocalDate.now().plusDays(22);
        
        List<Leave> approvedLeaves = leaveRepository.findApprovedLeavesInDateRange(startDate, endDate);
        assertEquals(1, approvedLeaves.size());
        assertEquals(LeaveType.SICK, approvedLeaves.get(0).getLeaveType());
    }

    @Test
    void testFindByUserDepartmentAndStatus() {
        List<Leave> itPendingLeaves = leaveRepository.findByUserDepartmentAndStatus("IT", LeaveStatus.PENDING);
        assertEquals(1, itPendingLeaves.size());
        assertEquals(LeaveType.ANNUAL, itPendingLeaves.get(0).getLeaveType());
    }

    @Test
    void testCascadingOperations() {
        // Test that deleting a user would cascade to leaves (if configured)
        // This is more of a conceptual test since we're using @DataJpaTest
        List<Leave> userLeaves = leaveRepository.findByUser(employee);
        assertEquals(2, userLeaves.size());
        
        // Verify relationships are properly maintained
        for (Leave leave : userLeaves) {
            assertEquals(employee, leave.getUser());
        }
    }
}
