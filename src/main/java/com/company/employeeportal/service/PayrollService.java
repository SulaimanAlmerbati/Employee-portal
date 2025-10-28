package com.company.employeeportal.service;

import com.company.employeeportal.model.Payroll;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.PayrollRepository;
import com.company.employeeportal.repository.UserRepository;
import com.company.employeeportal.exception.PayrollAccessException;
import com.company.employeeportal.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing payroll operations.
 * Handles payslip data retrieval, access control, and data retention.
 */
@Service
@Transactional
public class PayrollService {

    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);
    private static final int DATA_RETENTION_MONTHS = 24;

    private final PayrollRepository payrollRepository;
    private final UserRepository userRepository;

    @Autowired
    public PayrollService(PayrollRepository payrollRepository, UserRepository userRepository) {
        this.payrollRepository = payrollRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieve all payslips for the authenticated user.
     * Implements access control to prevent cross-user data access.
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public List<Payroll> getMyPayslips(String userEmail) {
        logger.info("Retrieving payslips for user: {}", userEmail);
        
        User user = findUserByEmail(userEmail);
        List<Payroll> payslips = payrollRepository.findByUserOrderByYearDescMonthDesc(user, Pageable.unpaged()).getContent();
        
        logger.info("Found {} payslips for user: {}", payslips.size(), userEmail);
        return payslips;
    }

    /**
     * Retrieve payslips for a user with pagination.
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public Page<Payroll> getMyPayslips(String userEmail, int page, int size) {
        logger.info("Retrieving paginated payslips for user: {} (page: {}, size: {})", userEmail, page, size);
        
        User user = findUserByEmail(userEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by("year").descending().and(Sort.by("month").descending()));
        
        Page<Payroll> payslips = payrollRepository.findByUserOrderByYearDescMonthDesc(user, pageable);
        
        logger.info("Found {} payslips (page {} of {}) for user: {}", 
                   payslips.getNumberOfElements(), payslips.getNumber() + 1, payslips.getTotalPages(), userEmail);
        return payslips;
    }

    /**
     * Retrieve a specific payslip by ID with access control.
     * Ensures users can only access their own payslips.
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public Payroll getPayslipById(Long payslipId, String userEmail) {
        logger.info("Retrieving payslip {} for user: {}", payslipId, userEmail);
        
        User user = findUserByEmail(userEmail);
        Optional<Payroll> payslip = payrollRepository.findById(payslipId);
        
        if (payslip.isEmpty()) {
            logger.warn("Payslip not found: {}", payslipId);
            throw new PayrollAccessException("Payslip not found");
        }
        
        // Verify the payslip belongs to the requesting user
        if (!payslip.get().getUser().getId().equals(user.getId())) {
            logger.warn("User {} attempted to access payslip {} belonging to another user", userEmail, payslipId);
            throw new PayrollAccessException("Access denied: You can only view your own payslips");
        }
        
        logger.info("Successfully retrieved payslip {} for user: {}", payslipId, userEmail);
        return payslip.get();
    }

    /**
     * Retrieve payslips for a specific year.
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public List<Payroll> getPayslipsByYear(String userEmail, Integer year) {
        logger.info("Retrieving payslips for user: {} and year: {}", userEmail, year);
        
        User user = findUserByEmail(userEmail);
        List<Payroll> payslips = payrollRepository.findByUserAndYear(user, year);
        
        logger.info("Found {} payslips for user: {} in year: {}", payslips.size(), userEmail, year);
        return payslips;
    }

    /**
     * Retrieve payslips within a date range (year range).
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public List<Payroll> getPayslipsByYearRange(String userEmail, Integer startYear, Integer endYear) {
        logger.info("Retrieving payslips for user: {} between years: {} - {}", userEmail, startYear, endYear);
        
        if (startYear > endYear) {
            throw new IllegalArgumentException("Start year cannot be greater than end year");
        }
        
        User user = findUserByEmail(userEmail);
        List<Payroll> payslips = payrollRepository.findByUserAndYearRange(user, startYear, endYear);
        
        logger.info("Found {} payslips for user: {} between years: {} - {}", 
                   payslips.size(), userEmail, startYear, endYear);
        return payslips;
    }

    /**
     * Retrieve recent payslips (within 24 months) for data retention compliance.
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public List<Payroll> getRecentPayslips(String userEmail) {
        logger.info("Retrieving recent payslips (last {} months) for user: {}", DATA_RETENTION_MONTHS, userEmail);
        
        User user = findUserByEmail(userEmail);
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(DATA_RETENTION_MONTHS);
        List<Payroll> payslips = payrollRepository.findRecentPayrollRecords(user, cutoffDate);
        
        logger.info("Found {} recent payslips for user: {}", payslips.size(), userEmail);
        return payslips;
    }

    /**
     * Get the latest payslip for a user.
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public Optional<Payroll> getLatestPayslip(String userEmail) {
        logger.info("Retrieving latest payslip for user: {}", userEmail);
        
        User user = findUserByEmail(userEmail);
        Optional<Payroll> latestPayslip = payrollRepository.findLatestByUser(user);
        
        if (latestPayslip.isPresent()) {
            logger.info("Found latest payslip for user: {} - {}/{}", 
                       userEmail, latestPayslip.get().getMonth(), latestPayslip.get().getYear());
        } else {
            logger.info("No payslips found for user: {}", userEmail);
        }
        
        return latestPayslip;
    }

    /**
     * Get payslip count for a user.
     */
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public long getPayslipCount(String userEmail) {
        User user = findUserByEmail(userEmail);
        long count = payrollRepository.countByUser(user);
        
        logger.info("User {} has {} payslips", userEmail, count);
        return count;
    }

    /**
     * Admin method to retrieve payslips for any user.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Payroll> getPayslipsForUser(Long userId) {
        logger.info("Admin retrieving payslips for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        List<Payroll> payslips = payrollRepository.findByUserOrderByYearDescMonthDesc(user, Pageable.unpaged()).getContent();
        
        logger.info("Admin found {} payslips for user ID: {}", payslips.size(), userId);
        return payslips;
    }

    /**
     * Scheduled method to clean up old payroll records for data retention compliance.
     * Runs monthly to remove records older than 24 months.
     */
    @Scheduled(cron = "0 0 2 1 * ?") // Run at 2 AM on the 1st day of every month
    public void cleanupOldPayrollRecords() {
        logger.info("Starting cleanup of old payroll records (older than {} months)", DATA_RETENTION_MONTHS);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(DATA_RETENTION_MONTHS);
        List<Payroll> recordsToDelete = payrollRepository.findPayrollRecordsToArchive(cutoffDate);
        
        if (!recordsToDelete.isEmpty()) {
            logger.info("Found {} payroll records to delete (older than {})", recordsToDelete.size(), cutoffDate);
            
            // Log the records being deleted for audit purposes
            recordsToDelete.forEach(record -> 
                logger.info("Deleting payroll record: ID={}, User={}, Period={}/{}", 
                           record.getId(), record.getUser().getEmail(), record.getMonth(), record.getYear()));
            
            payrollRepository.deleteOldPayrollRecords(cutoffDate);
            logger.info("Successfully deleted {} old payroll records", recordsToDelete.size());
        } else {
            logger.info("No old payroll records found for cleanup");
        }
    }

    /**
     * Helper method to find user by email with proper error handling.
     */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found with email: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }

    /**
     * Validate payslip access for the current user.
     */
    private void validatePayslipAccess(Payroll payslip, User user) {
        if (!payslip.getUser().getId().equals(user.getId())) {
            logger.warn("User {} attempted to access payslip {} belonging to user {}", 
                       user.getEmail(), payslip.getId(), payslip.getUser().getEmail());
            throw new PayrollAccessException("Access denied: You can only view your own payslips");
        }
    }
}