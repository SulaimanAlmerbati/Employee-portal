package com.company.employeeportal.repository;

import com.company.employeeportal.model.Announcement;
import com.company.employeeportal.model.AnnouncementReadStatus;
import com.company.employeeportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AnnouncementReadStatus entity operations.
 * Provides methods for tracking announcement read status by users.
 */
@Repository
public interface AnnouncementReadStatusRepository extends JpaRepository<AnnouncementReadStatus, Long> {

    /**
     * Find read status by announcement and user.
     */
    Optional<AnnouncementReadStatus> findByAnnouncementAndUser(Announcement announcement, User user);

    /**
     * Check if user has read a specific announcement.
     */
    boolean existsByAnnouncementAndUser(Announcement announcement, User user);

    /**
     * Find all read statuses for a specific user.
     */
    List<AnnouncementReadStatus> findByUser(User user);

    /**
     * Find all read statuses for a specific announcement.
     */
    List<AnnouncementReadStatus> findByAnnouncement(Announcement announcement);

    /**
     * Count how many users have read a specific announcement.
     */
    long countByAnnouncement(Announcement announcement);

    /**
     * Find announcement IDs that have been read by a specific user.
     */
    @Query("SELECT ars.announcement.id FROM AnnouncementReadStatus ars WHERE ars.user = :user")
    List<Long> findReadAnnouncementIdsByUser(@Param("user") User user);

    /**
     * Find users who have read a specific announcement.
     */
    @Query("SELECT ars.user FROM AnnouncementReadStatus ars WHERE ars.announcement = :announcement")
    List<User> findUsersWhoReadAnnouncement(@Param("announcement") Announcement announcement);

    /**
     * Delete read status records for a specific announcement.
     */
    void deleteByAnnouncement(Announcement announcement);

    /**
     * Delete read status records for a specific user.
     */
    void deleteByUser(User user);
}
