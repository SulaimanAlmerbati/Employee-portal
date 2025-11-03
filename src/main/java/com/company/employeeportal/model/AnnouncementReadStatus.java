package com.company.employeeportal.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing read status of announcements by employees.
 * Tracks which employees have read which announcements.
 */
@Entity
@Table(name = "announcement_read_status", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"announcement_id", "user_id"}))
public class AnnouncementReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "read_at", nullable = false, updatable = false)
    private LocalDateTime readAt;

    // Constructors
    public AnnouncementReadStatus() {}

    public AnnouncementReadStatus(Announcement announcement, User user) {
        this.announcement = announcement;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Announcement getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(Announcement announcement) {
        this.announcement = announcement;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    @Override
    public String toString() {
        return "AnnouncementReadStatus{" +
                "id=" + id +
                ", announcementId=" + (announcement != null ? announcement.getId() : null) +
                ", userId=" + (user != null ? user.getId() : null) +
                ", readAt=" + readAt +
                '}';
    }
}
