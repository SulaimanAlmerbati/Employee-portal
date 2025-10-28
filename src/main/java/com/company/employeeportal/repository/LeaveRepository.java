package com.company.employeeportal.repository;

import com.company.employeeportal.model.Leave;
import com.company.employeeportal.model.LeaveStatus;
import com.company.employeeportal.model.LeaveType;
import com.company.employeeportal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Leave entity operations.
 * Provides custom query methods for leave management functionality.
 */
@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    /**
     * Find all leaves for a specific user.
     */
    List<Leave> findByUser(User user);

    /**
     * Find leaves for a user with pagination.
     */
    Page<Leave> findByUser(User user, Pageable pageable);

    /**
     * Find leaves by user and status.
     */
    List<Leave> findByUserAndStatus(User user, LeaveStatus status);

    /**
     * Find leaves by status.
     */
    List<Leave> findByStatus(LeaveStatus status);

    /**
     * Find leaves by status with pagination.
     */
    Page<Leave> findByStatus(LeaveStatus status, Pageable pageable);

    /**
     * Find pending leaves for approval.
     */
    List<Leave> findByStatusOrderByCreatedAtAsc(LeaveStatus status);

    /**
     * Find leaves by user and date range.
     */
    @Query("SELECT l FROM Leave l WHERE l.user = :user AND " +
           "((l.startDate >= :startDate AND l.startDate <= :endDate) OR " +
           "(l.endDate >= :startDate AND l.endDate <= :endDate) OR " +
           "(l.startDate <= :startDate AND l.endDate >= :endDate))")
    List<Leave> findByUserAndDateRange(@Param("user") User user,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Find overlapping leaves for a user (to prevent conflicts).
     */
    @Query("SELECT l FROM Leave l WHERE l.user = :user AND l.status = 'APPROVED' AND " +
           "((l.startDate <= :endDate AND l.endDate >= :startDate))")
    List<Leave> findOverlappingApprovedLeaves(@Param("user") User user,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * Find leaves by leave type.
     */
    List<Leave> findByLeaveType(LeaveType leaveType);

    /**
     * Find leaves approved by a specific manager.
     */
    List<Leave> findByApprovedBy(User approvedBy);

    /**
     * Count pending leaves.
     */
    long countByStatus(LeaveStatus status);

    /**
     * Count leaves by user and status.
     */
    long countByUserAndStatus(User user, LeaveStatus status);

    /**
     * Find leaves by user and year.
     */
    @Query("SELECT l FROM Leave l WHERE l.user = :user AND " +
           "(YEAR(l.startDate) = :year OR YEAR(l.endDate) = :year)")
    List<Leave> findByUserAndYear(@Param("user") User user, @Param("year") int year);

    /**
     * Find recent leaves (last 30 days).
     */
    @Query("SELECT l FROM Leave l WHERE l.createdAt >= :thirtyDaysAgo ORDER BY l.createdAt DESC")
    List<Leave> findRecentLeaves(@Param("thirtyDaysAgo") LocalDate thirtyDaysAgo);

    /**
     * Find approved leaves within date range (for calendar view).
     */
    @Query("SELECT l FROM Leave l WHERE l.status = 'APPROVED' AND " +
           "l.startDate <= :endDate AND l.endDate >= :startDate " +
           "ORDER BY l.startDate")
    List<Leave> findApprovedLeavesInDateRange(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * Find leaves by department (through user relationship).
     */
    @Query("SELECT l FROM Leave l WHERE l.user.department = :department")
    List<Leave> findByUserDepartment(@Param("department") String department);

    /**
     * Find leaves by department and status.
     */
    @Query("SELECT l FROM Leave l WHERE l.user.department = :department AND l.status = :status")
    List<Leave> findByUserDepartmentAndStatus(@Param("department") String department,
                                              @Param("status") LeaveStatus status);

    /**
     * Count leaves by leave type.
     */
    long countByLeaveType(LeaveType leaveType);
}