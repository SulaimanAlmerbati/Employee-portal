package com.company.employeeportal.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for creating and updating announcements.
 */
public class AnnouncementRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    // Constructors
    public AnnouncementRequest() {}

    public AnnouncementRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "AnnouncementRequest{" +
                "title='" + title + '\'' +
                ", content='" + (content != null && content.length() > 50 ? 
                    content.substring(0, 50) + "..." : content) + '\'' +
                '}';
    }
}
