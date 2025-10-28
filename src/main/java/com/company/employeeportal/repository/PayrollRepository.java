package com.company.employeeportal.repository;

import com.company.employeeportal.model.Payroll;
import com.company.employeeportal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payroll entity operations.
 * Provides custom query methods for payroll management functionality.
 */
@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    /**
     * Find all payroll records for a specific user.
     */
    List<Payroll> findByUser(User user);

    /**
     * Find payroll records for a user with pagination.
     */
    Page<Payroll> findByUser(User user, Pageable pageable);

    /**
     * Find payroll records ordered by year and month descending.
     */
    Page<Payroll> findByUserOrderByYearDescMonthDesc(User user, Pageable pageable);

    /**
     * Find payroll record for a specific user, month, and year.
     */
    Optional<Payroll> findByUserAndMonthAndYear(User user, Integer month, Integer year);

    /**
     * Find payroll records for a user within a year.
     */
    List<Payroll> findByUserAndYear(User user, Integer year);

    /**
     * Find payroll records for a user within a year range.
     */
    @Query("SELECT p FROM Payroll p WHERE p.user = :user AND " +
           "p.year BETWEEN :startYear AND :endYear " +
           "ORDER BY p.year DESC, p.month DESC")
    List<Payroll> findByUserAndYearRange(@Param("user") User user,
                                         @Param("startYear") Integer startYear,
                                         @Param("endYear") Integer endYear);

    /**
     * Find payroll records for a specific month and year.
     */
    List<Payroll> findByMonthAndYear(Integer month, Integer year);

    /**
     * Find recent payroll records for a user (last 24 months).
     */
    @Query("SELECT p FROM Payroll p WHERE p.user = :user AND " +
           "p.createdAt >= :twentyFourMonthsAgo " +
           "ORDER BY p.year DESC, p.month DESC")
    List<Payroll> findRecentPayrollRecords(@Param("user") User user,
                                           @Param("twentyFourMonthsAgo") LocalDateTime twentyFourMonthsAgo);

    /**
     * Check if payroll exists for user in specific month/year.
     */
    boolean existsByUserAndMonthAndYear(User user, Integer month, Integer year);

    /**
     * Find payroll records by year.
     */
    List<Payroll> findByYear(Integer year);

    /**
     * Find payroll records by year with pagination.
     */
    Page<Payroll> findByYear(Integer year, Pageable pageable);

    /**
     * Count payroll records for a user.
     */
    long countByUser(User user);

    /**
     * Find latest payroll record for a user.
     */
    @Query("SELECT p FROM Payroll p WHERE p.user = :user " +
           "ORDER BY p.year DESC, p.month DESC LIMIT 1")
    Optional<Payroll> findLatestByUser(@Param("user") User user);

    /**
     * Calculate total gross pay for a user in a year.
     */
    @Query("SELECT SUM(p.grossPay) FROM Payroll p WHERE p.user = :user AND p.year = :year")
    Optional<Double> calculateTotalGrossPayForUserAndYear(@Param("user") User user, 
                                                          @Param("year") Integer year);

    /**
     * Calculate total net pay for a user in a year.
     */
    @Query("SELECT SUM(p.netPay) FROM Payroll p WHERE p.user = :user AND p.year = :year")
    Optional<Double> calculateTotalNetPayForUserAndYear(@Param("user") User user, 
                                                        @Param("year") Integer year);

    /**
     * Find payroll records that need to be archived (older than 24 months).
     */
    @Query("SELECT p FROM Payroll p WHERE p.createdAt < :cutoffDate")
    List<Payroll> findPayrollRecordsToArchive(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old payroll records (for data retention compliance).
     */
    @Query("DELETE FROM Payroll p WHERE p.createdAt < :cutoffDate")
    void deleteOldPayrollRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
}