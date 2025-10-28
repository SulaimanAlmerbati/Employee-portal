package com.company.employeeportal.controller;

import com.company.employeeportal.model.Payroll;
import com.company.employeeportal.service.PayrollService;
import com.company.employeeportal.service.PdfGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for payroll management operations.
 * Provides endpoints for employees to view and download their payslips.
 */
@RestController
@RequestMapping("/api/payroll")
@Validated
@Tag(name = "Payroll", description = "Payroll management operations")
public class PayrollController {

    private static final Logger logger = LoggerFactory.getLogger(PayrollController.class);

    private final PayrollService payrollService;
    private final PdfGenerationService pdfGenerationService;

    @Autowired
    public PayrollController(PayrollService payrollService, PdfGenerationService pdfGenerationService) {
        this.payrollService = payrollService;
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * Get all payslips for the authenticated user.
     */
    @GetMapping("/my-payslips")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get my payslips", description = "Retrieve all payslips for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payslips retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Payroll>> getMyPayslips(Authentication authentication) {
        logger.info("User {} requesting all payslips", authentication.getName());
        
        List<Payroll> payslips = payrollService.getMyPayslips(authentication.getName());
        
        logger.info("Returning {} payslips for user {}", payslips.size(), authentication.getName());
        return ResponseEntity.ok(payslips);
    }

    /**
     * Get paginated payslips for the authenticated user.
     */
    @GetMapping("/my-payslips/paginated")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get paginated payslips", description = "Retrieve paginated payslips for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payslips retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<Payroll>> getMyPayslipsPaginated(
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            Authentication authentication) {
        
        logger.info("User {} requesting paginated payslips (page: {}, size: {})", 
                   authentication.getName(), page, size);
        
        Page<Payroll> payslips = payrollService.getMyPayslips(authentication.getName(), page, size);
        
        logger.info("Returning page {} of {} payslips for user {}", 
                   page + 1, payslips.getTotalPages(), authentication.getName());
        return ResponseEntity.ok(payslips);
    }

    /**
     * Get a specific payslip by ID.
     */
    @GetMapping("/payslip/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get payslip by ID", description = "Retrieve a specific payslip by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payslip retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - not your payslip"),
        @ApiResponse(responseCode = "404", description = "Payslip not found")
    })
    public ResponseEntity<Payroll> getPayslipById(
            @Parameter(description = "Payslip ID") 
            @PathVariable Long id,
            Authentication authentication) {
        
        logger.info("User {} requesting payslip with ID: {}", authentication.getName(), id);
        
        Payroll payslip = payrollService.getPayslipById(id, authentication.getName());
        
        logger.info("Successfully retrieved payslip {} for user {}", id, authentication.getName());
        return ResponseEntity.ok(payslip);
    }

    /**
     * Get payslips for a specific year.
     */
    @GetMapping("/my-payslips/year/{year}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get payslips by year", description = "Retrieve payslips for a specific year")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payslips retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid year"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Payroll>> getPayslipsByYear(
            @Parameter(description = "Year (e.g., 2024)") 
            @PathVariable @Min(2020) @Max(2100) Integer year,
            Authentication authentication) {
        
        logger.info("User {} requesting payslips for year: {}", authentication.getName(), year);
        
        List<Payroll> payslips = payrollService.getPayslipsByYear(authentication.getName(), year);
        
        logger.info("Returning {} payslips for user {} in year {}", 
                   payslips.size(), authentication.getName(), year);
        return ResponseEntity.ok(payslips);
    }

    /**
     * Get payslips within a year range.
     */
    @GetMapping("/my-payslips/range")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get payslips by year range", description = "Retrieve payslips within a year range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payslips retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid year range"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Payroll>> getPayslipsByYearRange(
            @Parameter(description = "Start year") 
            @RequestParam @Min(2020) @Max(2100) Integer startYear,
            @Parameter(description = "End year") 
            @RequestParam @Min(2020) @Max(2100) Integer endYear,
            Authentication authentication) {
        
        logger.info("User {} requesting payslips for year range: {} - {}", 
                   authentication.getName(), startYear, endYear);
        
        List<Payroll> payslips = payrollService.getPayslipsByYearRange(
                authentication.getName(), startYear, endYear);
        
        logger.info("Returning {} payslips for user {} in year range {} - {}", 
                   payslips.size(), authentication.getName(), startYear, endYear);
        return ResponseEntity.ok(payslips);
    }

    /**
     * Get recent payslips (within 24 months).
     */
    @GetMapping("/my-payslips/recent")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get recent payslips", description = "Retrieve recent payslips within 24 months")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent payslips retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Payroll>> getRecentPayslips(Authentication authentication) {
        logger.info("User {} requesting recent payslips", authentication.getName());
        
        List<Payroll> payslips = payrollService.getRecentPayslips(authentication.getName());
        
        logger.info("Returning {} recent payslips for user {}", payslips.size(), authentication.getName());
        return ResponseEntity.ok(payslips);
    }

    /**
     * Get the latest payslip.
     */
    @GetMapping("/my-payslips/latest")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get latest payslip", description = "Retrieve the most recent payslip")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Latest payslip retrieved successfully"),
        @ApiResponse(responseCode = "204", description = "No payslips found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Payroll> getLatestPayslip(Authentication authentication) {
        logger.info("User {} requesting latest payslip", authentication.getName());
        
        Optional<Payroll> latestPayslip = payrollService.getLatestPayslip(authentication.getName());
        
        if (latestPayslip.isPresent()) {
            logger.info("Returning latest payslip for user {}: {}/{}", 
                       authentication.getName(), latestPayslip.get().getMonth(), latestPayslip.get().getYear());
            return ResponseEntity.ok(latestPayslip.get());
        } else {
            logger.info("No payslips found for user {}", authentication.getName());
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * Get payslip count for the user.
     */
    @GetMapping("/my-payslips/count")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get payslip count", description = "Get the total number of payslips for the user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payslip count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Long> getPayslipCount(Authentication authentication) {
        logger.info("User {} requesting payslip count", authentication.getName());
        
        long count = payrollService.getPayslipCount(authentication.getName());
        
        logger.info("User {} has {} payslips", authentication.getName(), count);
        return ResponseEntity.ok(count);
    }

    /**
     * Download payslip as PDF.
     */
    @GetMapping("/payslip/{id}/download")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Download payslip PDF", description = "Download a payslip as PDF document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF generated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - not your payslip"),
        @ApiResponse(responseCode = "404", description = "Payslip not found"),
        @ApiResponse(responseCode = "500", description = "PDF generation failed")
    })
    public ResponseEntity<byte[]> downloadPayslipPdf(
            @Parameter(description = "Payslip ID") 
            @PathVariable Long id,
            Authentication authentication) {
        
        logger.info("User {} requesting PDF download for payslip: {}", authentication.getName(), id);
        
        try {
            // First verify the user has access to this payslip
            Payroll payslip = payrollService.getPayslipById(id, authentication.getName());
            
            // Generate PDF
            byte[] pdfBytes = pdfGenerationService.generatePayslipPdf(payslip);
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    String.format("payslip_%s_%02d_%d.pdf", 
                                 payslip.getUser().getName().replaceAll("\\s+", "_"),
                                 payslip.getMonth(), 
                                 payslip.getYear()));
            headers.setContentLength(pdfBytes.length);
            
            logger.info("Successfully generated and prepared PDF download for payslip {} (user: {}, size: {} bytes)", 
                       id, authentication.getName(), pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Failed to generate PDF for payslip {} (user: {})", id, authentication.getName(), e);
            throw new RuntimeException("Failed to generate PDF payslip", e);
        }
    }

    /**
     * Admin endpoint to get payslips for any user.
     */
    @GetMapping("/admin/user/{userId}/payslips")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get user payslips", description = "Admin endpoint to retrieve payslips for any user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payslips retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<Payroll>> getPayslipsForUser(
            @Parameter(description = "User ID") 
            @PathVariable Long userId,
            Authentication authentication) {
        
        logger.info("Admin {} requesting payslips for user ID: {}", authentication.getName(), userId);
        
        List<Payroll> payslips = payrollService.getPayslipsForUser(userId);
        
        logger.info("Admin {} retrieved {} payslips for user ID: {}", 
                   authentication.getName(), payslips.size(), userId);
        return ResponseEntity.ok(payslips);
    }
}