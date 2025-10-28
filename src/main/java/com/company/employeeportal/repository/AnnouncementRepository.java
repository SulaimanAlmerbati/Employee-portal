package com.company.employeeportal.repository;

import com.company.employeeportal.model.Announcement;
import com.company.employeeportal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Announcement entity operations.
 * Provides custom query methods for announcement management functionality.
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * Find all active announcements.
     */
    List<Announcement> findByActiveTrue();

    /**
     * Find active announcements with pagination.
     */
    Page<Announcement> findByActiveTrue(Pageable pageable);

    /**
     * Find active announcements ordered by creation date descending.
     */
    List<Announcement> findByActiveTrueOrderByCreatedAtDesc();

    /**
     * Find active announcements with pagination ordered by creation date descending.
     */
    Page<Announcement> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find announcements by creator.
     */
    List<Announcement> findByCreatedBy(User createdBy);

    /**
     * Find active announcements by creator.
     */
    List<Announcement> findByCreatedByAndActiveTrue(User createdBy);

    /**
     * Find announcements by creator with pagination.
     */
    Page<Announcement> findByCreatedBy(User createdBy, Pageable pageable);

    /**
     * Find all announcements (active and inactive) with pagination.
     */
    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find recent active announcements (last 30 days).
     */
    @Query("SELECT a FROM Announcement a WHERE a.active = true AND " +
           "a.createdAt >= :thirtyDaysAgo ORDER BY a.createdAt DESC")
    List<Announcement> findRecentActiveAnnouncements(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

    /**
     * Find announcements by title containing search term.
     */
    @Query("SELECT a FROM Announcement a WHERE " +
           "LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
           "a.active = :active ORDER BY a.createdAt DESC")
    Page<Announcement> findByTitleContainingIgnoreCase(@Param("searchTerm") String searchTerm,
                                                       @Param("active") Boolean active,
                                                       Pageable pageable);

    /**
     * Search announcements by title or content.
     */
    @Query("SELECT a FROM Announcement a WHERE " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "a.active = :active ORDER BY a.createdAt DESC")
    Page<Announcement> searchByTitleOrContent(@Param("searchTerm") String searchTerm,
                                              @Param("active") Boolean active,
                                              Pageable pageable);

    /**
     * Count active announcements.
     */
    long countByActiveTrue();

    /**
     * Count total announcements.
     */
    long count();

    /**
     * Count announcements by creator.
     */
    long countByCreatedBy(User createdBy);

    /**
     * Count active announcements by creator.
     */
    long countByCreatedByAndActiveTrue(User createdBy);

    /**
     * Find announcements created within date range.
     */
    @Query("SELECT a FROM Announcement a WHERE " +
           "a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    List<Announcement> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find active announcements created within date range.
     */
    @Query("SELECT a FROM Announcement a WHERE a.active = true AND " +
           "a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    List<Announcement> findActiveByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find top N recent active announcements for dashboard.
     */
    @Query("SELECT a FROM Announcement a WHERE a.active = true " +
           "ORDER BY a.createdAt DESC LIMIT :limit")
    List<Announcement> findTopRecentActiveAnnouncements(@Param("limit") int limit);
}