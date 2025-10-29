package com.company.employeeportal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing payroll information for an employee.
 * Contains salary details and deductions for a specific pay period.
 */
@Entity
@Table(name = "payroll", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "month", "year"}))
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Column(nullable = false)
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be 2020 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    @Column(nullable = false)
    private Integer year;

    @NotNull(message = "Gross pay is required")
    @DecimalMin(value = "0.00", message = "Gross pay must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Gross pay must have at most 10 integer digits and 2 decimal places")
    @Column(name = "gross_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossPay;

    @NotNull(message = "Net pay is required")
    @DecimalMin(value = "0.00", message = "Net pay must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Net pay must have at most 10 integer digits and 2 decimal places")
    @Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPay;

    @NotNull(message = "Deductions is required")
    @DecimalMin(value = "0.00", message = "Deductions must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Deductions must have at most 10 integer digits and 2 decimal places")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductions;

    @Size(max = 1000, message = "Deduction details must not exceed 1000 characters")
    @Column(name = "deduction_details", length = 1000)
    private String deductionDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Payroll() {}

    public Payroll(User user, Integer month, Integer year, BigDecimal grossPay, 
                   BigDecimal netPay, BigDecimal deductions) {
        this.user = user;
        this.month = month;
        this.year = year;
        this.grossPay = grossPay;
        this.netPay = netPay;
        this.deductions = deductions;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(BigDecimal grossPay) {
        this.grossPay = grossPay;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public void setNetPay(BigDecimal netPay) {
        this.netPay = netPay;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public String getDeductionDetails() {
        return deductionDetails;
    }

    public void setDeductionDetails(String deductionDetails) {
        this.deductionDetails = deductionDetails;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public String getPayPeriod() {
        return String.format("%02d/%d", month, year);
    }

    public BigDecimal getTaxAmount() {
        return grossPay.subtract(netPay).subtract(deductions);
    }

    // Custom validation method
    @AssertTrue(message = "Net pay must be less than or equal to gross pay")
    public boolean isNetPayValid() {
        return netPay == null || grossPay == null || netPay.compareTo(grossPay) <= 0;
    }

    @AssertTrue(message = "Deductions must be less than or equal to gross pay")
    public boolean isDeductionsValid() {
        return deductions == null || grossPay == null || deductions.compareTo(grossPay) <= 0;
    }

    @Override
    public String toString() {
        return "Payroll{" +
                "id=" + id +
                ", month=" + month +
                ", year=" + year +
                ", grossPay=" + grossPay +
                ", netPay=" + netPay +
                ", deductions=" + deductions +
                ", user=" + (user != null ? user.getName() : null) +
                '}';
    }
}
