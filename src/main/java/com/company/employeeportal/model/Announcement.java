package com.company.employeeportal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing company announcements in the Employee Portal System.
 * Contains announcement content, creator information, and publication status.
 */
@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    @Column(nullable = false, length = 5000)
    private String content;

    @NotNull(message = "Creator is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Size(max = 100, message = "Target department must not exceed 100 characters")
    @Column(name = "target_department", length = 100)
    private String targetDepartment; // null means company-wide

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Announcement() {}

    public Announcement(String title, String content, User createdBy) {
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        this.active = true;
    }

    public Announcement(String title, String content, User createdBy, String targetDepartment) {
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        this.targetDepartment = targetDepartment;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTargetDepartment() {
        return targetDepartment;
    }

    public void setTargetDepartment(String targetDepartment) {
        this.targetDepartment = targetDepartment;
    }

    // Helper methods
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public String getCreatorName() {
        return createdBy != null ? createdBy.getName() : "Unknown";
    }

    public String getPreview() {
        if (content == null) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }

    public boolean isVisibleToUser(User user) {
        // If targetDepartment is null or empty, it's company-wide
        if (targetDepartment == null || targetDepartment.trim().isEmpty()) {
            return true;
        }
        
        // If user has no department, they can only see company-wide announcements
        if (user.getDepartment() == null) {
            return false;
        }
        
        // Check if user's department matches the target department
        return targetDepartment.equals(user.getDepartment());
    }

    public String getTargetDescription() {
        if (targetDepartment == null || targetDepartment.trim().isEmpty()) {
            return "Company-wide";
        }
        return targetDepartment;
    }

    @Override
    public String toString() {
        return "Announcement{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", active=" + active +
                ", createdBy=" + (createdBy != null ? createdBy.getName() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}
