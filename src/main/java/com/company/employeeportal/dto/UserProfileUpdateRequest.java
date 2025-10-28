package com.company.employeeportal.dto;

import com.company.employeeportal.model.Role;
import com.company.employeeportal.validation.UniqueEmailForUpdate;
import com.company.employeeportal.validation.ValidContactInfo;
import com.company.employeeportal.validation.ValidName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * DTO for user profile update requests.
 * Contains validation annotations for input validation including email uniqueness check.
 */
@UniqueEmailForUpdate(emailField = "email", userIdField = "userId")
public class UserProfileUpdateRequest {

    @JsonIgnore
    private Long userId; // Used for validation, not exposed in JSON

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @ValidName(allowEmpty = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    private Role role;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Position must not exceed 100 characters")
    private String position;

    @Size(max = 200, message = "Contact info must not exceed 200 characters")
    @ValidContactInfo
    private String contactInfo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinDate;

    // Constructors
    public UserProfileUpdateRequest() {}

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }
}