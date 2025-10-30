package com.company.employeeportal.controller;

import com.company.employeeportal.dto.AnnouncementRequest;
import com.company.employeeportal.dto.AnnouncementResponse;
import com.company.employeeportal.security.UserPrincipal;
import com.company.employeeportal.service.AnnouncementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for announcement management operations.
 * Handles announcement viewing for employees and management for admins.
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);

    private final AnnouncementService announcementService;

    @Autowired
    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * Get active announcements for employees with pagination.
     * Ordered by creation date descending (most recent first).
     */
    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<Page<AnnouncementResponse>> getActiveAnnouncements(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Retrieving active announcements for user: {}", userPrincipal.getUsername());
        
        Page<AnnouncementResponse> announcements = announcementService.getActiveAnnouncements(
                pageable, userPrincipal.getId());
        
        return ResponseEntity.ok(announcements);
    }

    /**
     * Get recent announcements for dashboard (last 5).
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<List<AnnouncementResponse>> getRecentAnnouncements(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Retrieving recent announcements for user: {}", userPrincipal.getUsername());
        
        List<AnnouncementResponse> announcements = announcementService.getRecentAnnouncements(
                userPrincipal.getId());
        
        return ResponseEntity.ok(announcements);
    }

    /**
     * Get announcement by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<AnnouncementResponse> getAnnouncementById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Retrieving announcement {} for user: {}", id, userPrincipal.getUsername());
        
        AnnouncementResponse announcement = announcementService.getAnnouncementById(
                id, userPrincipal.getId());
        
        return ResponseEntity.ok(announcement);
    }

    /**
     * Search announcements by title or content.
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<Page<AnnouncementResponse>> searchAnnouncements(
            @RequestParam String query,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Searching announcements with query: {} for user: {}", query, userPrincipal.getUsername());
        
        Page<AnnouncementResponse> announcements = announcementService.searchAnnouncements(
                query, activeOnly, pageable, userPrincipal.getId());
        
        return ResponseEntity.ok(announcements);
    }

    /**
     * Mark an announcement as read.
     */
    @PostMapping("/{id}/mark-read")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<Void> markAnnouncementAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Marking announcement {} as read for user: {}", id, userPrincipal.getUsername());
        
        announcementService.markAnnouncementAsRead(id, userPrincipal.getId());
        
        return ResponseEntity.ok().build();
    }

    /**
     * Check if an announcement has been read by the current user.
     */
    @GetMapping("/{id}/read-status")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('HR') or hasRole('IT_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<Boolean> getReadStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        boolean isRead = announcementService.isAnnouncementReadByUser(id, userPrincipal.getId());
        
        return ResponseEntity.ok(isRead);
    }

    // Admin-only endpoints

    /**
     * Get all announcements (active and inactive) for HR with pagination.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Page<AnnouncementResponse>> getAllAnnouncements(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Admin retrieving all announcements: {}", userPrincipal.getUsername());
        
        Page<AnnouncementResponse> announcements = announcementService.getAllAnnouncements(
                pageable, userPrincipal.getId());
        
        return ResponseEntity.ok(announcements);
    }

    /**
     * Create a new announcement (HR only).
     */
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Creating announcement by admin: {}", userPrincipal.getUsername());
        
        AnnouncementResponse response = announcementService.createAnnouncement(
                request, userPrincipal.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing announcement (HR only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Updating announcement {} by admin: {}", id, userPrincipal.getUsername());
        
        AnnouncementResponse response = announcementService.updateAnnouncement(
                id, request, userPrincipal.getId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate an announcement (soft delete) (HR only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Void> deactivateAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Deactivating announcement {} by admin: {}", id, userPrincipal.getUsername());
        
        announcementService.deactivateAnnouncement(id, userPrincipal.getId());
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivate an announcement (HR only).
     */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Void> reactivateAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Reactivating announcement {} by admin: {}", id, userPrincipal.getUsername());
        
        announcementService.reactivateAnnouncement(id, userPrincipal.getId());
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get announcement statistics for HR dashboard.
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<AnnouncementService.AnnouncementStatistics> getAnnouncementStatistics() {
        
        logger.debug("Retrieving announcement statistics for admin dashboard");
        
        AnnouncementService.AnnouncementStatistics stats = announcementService.getAnnouncementStatistics();
        
        return ResponseEntity.ok(stats);
    }
}