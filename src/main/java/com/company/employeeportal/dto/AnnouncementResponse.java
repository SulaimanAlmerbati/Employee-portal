package com.company.employeeportal.dto;

import java.time.LocalDateTime;

/**
 * DTO for announcement response data.
 */
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String content;
    private String createdByName;
    private Long createdById;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String preview;
    private Boolean isRead;
    private String targetDepartment;

    // Constructors
    public AnnouncementResponse() {}

    public AnnouncementResponse(Long id, String title, String content, String createdByName,
                               Long createdById, Boolean active, LocalDateTime createdAt,
                               LocalDateTime updatedAt, String targetDepartment) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdByName = createdByName;
        this.createdById = createdById;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.targetDepartment = targetDepartment;
        this.preview = generatePreview(content);
        this.isRead = false; // Default to unread
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
        this.preview = generatePreview(content);
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
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

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
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

    private String generatePreview(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }

    public String getTargetDescription() {
        if (targetDepartment == null || targetDepartment.trim().isEmpty()) {
            return "Company-wide";
        }
        return targetDepartment;
    }

    @Override
    public String toString() {
        return "AnnouncementResponse{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", createdByName='" + createdByName + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}