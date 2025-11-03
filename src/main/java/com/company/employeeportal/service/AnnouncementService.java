package com.company.employeeportal.service;

import com.company.employeeportal.dto.AnnouncementRequest;
import com.company.employeeportal.dto.AnnouncementResponse;
import com.company.employeeportal.exception.AnnouncementNotFoundException;
import com.company.employeeportal.exception.UnauthorizedAccessException;
import com.company.employeeportal.exception.UserNotFoundException;
import com.company.employeeportal.model.Announcement;
import com.company.employeeportal.model.AnnouncementReadStatus;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.AnnouncementReadStatusRepository;
import com.company.employeeportal.repository.AnnouncementRepository;
import com.company.employeeportal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for announcement management operations.
 * Handles announcement creation, retrieval, updates, and read status tracking.
 */
@Service
@Transactional
public class AnnouncementService {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;

    @Autowired
    public AnnouncementService(AnnouncementRepository announcementRepository,
                              AnnouncementReadStatusRepository readStatusRepository,
                              UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.readStatusRepository = readStatusRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new announcement (Admin only).
     * 
     * @param request the announcement request
     * @param creatorId the ID of the user creating the announcement
     * @return the created announcement response
     * @throws UserNotFoundException if creator not found
     * @throws UnauthorizedAccessException if user is not admin
     */
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request, Long creatorId) {
        logger.info("Creating announcement with title: {} by user ID: {}", request.getTitle(), creatorId);

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + creatorId));

        if (creator.getRole() != Role.HR) {
            throw new UnauthorizedAccessException("Only HR can create announcements");
        }

        Announcement announcement = new Announcement(request.getTitle(), request.getContent(), creator, request.getTargetDepartment());
        announcement = announcementRepository.save(announcement);

        logger.info("Successfully created announcement with ID: {}", announcement.getId());
        return convertToResponse(announcement, null);
    }

    /**
     * Get all active announcements with pagination for employees.
     * 
     * @param pageable pagination information
     * @param userId the ID of the requesting user (for read status)
     * @return page of announcement responses
     */
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getActiveAnnouncements(Pageable pageable, Long userId) {
        logger.debug("Retrieving active announcements for user ID: {}", userId);

        // Get the user to check their department
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        Page<Announcement> announcements = announcementRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
        
        // Get read status for the user
        Set<Long> readAnnouncementIds = null;
        if (user != null) {
            readAnnouncementIds = readStatusRepository.findReadAnnouncementIdsByUser(user)
                    .stream().collect(Collectors.toSet());
        }

        final Set<Long> finalReadIds = readAnnouncementIds;
        final User finalUser = user;
        
        // Filter announcements based on user's department and convert to response
        List<AnnouncementResponse> responses = announcements.getContent().stream()
                .filter(announcement -> finalUser == null || announcement.isVisibleToUser(finalUser))
                .map(announcement -> convertToResponse(announcement, finalReadIds))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, responses.size());
    }

    /**
     * Get all announcements (active and inactive) with pagination for admin.
     * 
     * @param pageable pagination information
     * @param requesterId the ID of the requesting user
     * @return page of announcement responses
     * @throws UnauthorizedAccessException if user is not admin
     */
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAllAnnouncements(Pageable pageable, Long requesterId) {
        logger.debug("Retrieving all announcements for admin user ID: {}", requesterId);

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + requesterId));

        if (requester.getRole() != Role.HR) {
            throw new UnauthorizedAccessException("Only HR can view all announcements");
        }

        Page<Announcement> announcements = announcementRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        List<AnnouncementResponse> responses = announcements.getContent().stream()
                .map(announcement -> convertToResponse(announcement, null))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, announcements.getTotalElements());
    }

    /**
     * Get recent active announcements for dashboard (last 5).
     * 
     * @param userId the ID of the requesting user (for read status)
     * @return list of recent announcement responses
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getRecentAnnouncements(Long userId) {
        logger.debug("Retrieving recent announcements for user ID: {}", userId);

        List<Announcement> announcements = announcementRepository.findTopRecentActiveAnnouncements(5);
        
        // Get read status for the user
        Set<Long> readAnnouncementIds = null;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                readAnnouncementIds = readStatusRepository.findReadAnnouncementIdsByUser(user)
                        .stream().collect(Collectors.toSet());
            }
        }

        final Set<Long> finalReadIds = readAnnouncementIds;
        return announcements.stream()
                .map(announcement -> convertToResponse(announcement, finalReadIds))
                .collect(Collectors.toList());
    }

    /**
     * Get announcement by ID.
     * 
     * @param id the announcement ID
     * @param userId the ID of the requesting user (for read status)
     * @return the announcement response
     * @throws AnnouncementNotFoundException if announcement not found
     */
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(Long id, Long userId) {
        logger.debug("Retrieving announcement with ID: {} for user ID: {}", id, userId);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found with ID: " + id));

        // Get read status for the user
        Set<Long> readAnnouncementIds = null;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                readAnnouncementIds = readStatusRepository.findReadAnnouncementIdsByUser(user)
                        .stream().collect(Collectors.toSet());
            }
        }

        return convertToResponse(announcement, readAnnouncementIds);
    }

    /**
     * Update an existing announcement (Admin only).
     * 
     * @param id the announcement ID
     * @param request the update request
     * @param updaterId the ID of the user updating the announcement
     * @return the updated announcement response
     * @throws AnnouncementNotFoundException if announcement not found
     * @throws UnauthorizedAccessException if user is not admin
     */
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request, Long updaterId) {
        logger.info("Updating announcement with ID: {} by user ID: {}", id, updaterId);

        User updater = userRepository.findById(updaterId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + updaterId));

        if (updater.getRole() != Role.HR) {
            throw new UnauthorizedAccessException("Only HR can update announcements");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found with ID: " + id));

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setTargetDepartment(request.getTargetDepartment());
        announcement = announcementRepository.save(announcement);

        logger.info("Successfully updated announcement with ID: {}", id);
        return convertToResponse(announcement, null);
    }

    /**
     * Delete an announcement (hard delete) (HR only).
     * 
     * @param id the announcement ID
     * @param deleterId the ID of the user deleting the announcement
     * @throws AnnouncementNotFoundException if announcement not found
     * @throws UnauthorizedAccessException if user is not admin
     */
    public void deleteAnnouncement(Long id, Long deleterId) {
        logger.info("Deleting announcement with ID: {} by user ID: {}", id, deleterId);

        User deleter = userRepository.findById(deleterId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + deleterId));

        if (deleter.getRole() != Role.HR) {
            throw new UnauthorizedAccessException("Only HR can delete announcements");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found with ID: " + id));

        // Delete related announcement read status records first
        readStatusRepository.deleteByAnnouncementId(id);
        
        // Then delete the announcement
        announcementRepository.delete(announcement);

        logger.info("Successfully deleted announcement with ID: {}", id);
    }

    /**
     * Deactivate an announcement (soft delete) (Admin only).
     * 
     * @param id the announcement ID
     * @param deleterId the ID of the user deleting the announcement
     * @throws AnnouncementNotFoundException if announcement not found
     * @throws UnauthorizedAccessException if user is not admin
     */
    public void deactivateAnnouncement(Long id, Long deleterId) {
        logger.info("Deactivating announcement with ID: {} by user ID: {}", id, deleterId);

        User deleter = userRepository.findById(deleterId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + deleterId));

        if (deleter.getRole() != Role.HR) {
            throw new UnauthorizedAccessException("Only HR can delete announcements");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found with ID: " + id));

        announcement.deactivate();
        announcementRepository.save(announcement);

        logger.info("Successfully deactivated announcement with ID: {}", id);
    }

    /**
     * Reactivate an announcement (Admin only).
     * 
     * @param id the announcement ID
     * @param activatorId the ID of the user reactivating the announcement
     * @throws AnnouncementNotFoundException if announcement not found
     * @throws UnauthorizedAccessException if user is not admin
     */
    public void reactivateAnnouncement(Long id, Long activatorId) {
        logger.info("Reactivating announcement with ID: {} by user ID: {}", id, activatorId);

        User activator = userRepository.findById(activatorId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + activatorId));

        if (activator.getRole() != Role.HR) {
            throw new UnauthorizedAccessException("Only HR can reactivate announcements");
        }

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found with ID: " + id));

        announcement.activate();
        announcementRepository.save(announcement);

        logger.info("Successfully reactivated announcement with ID: {}", id);
    }

    /**
     * Mark an announcement as read by a user.
     * 
     * @param announcementId the announcement ID
     * @param userId the user ID
     * @throws AnnouncementNotFoundException if announcement not found
     * @throws UserNotFoundException if user not found
     */
    public void markAnnouncementAsRead(Long announcementId, Long userId) {
        logger.debug("Marking announcement ID: {} as read by user ID: {}", announcementId, userId);

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found with ID: " + announcementId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Check if already marked as read
        if (!readStatusRepository.existsByAnnouncementAndUser(announcement, user)) {
            AnnouncementReadStatus readStatus = new AnnouncementReadStatus(announcement, user);
            readStatusRepository.save(readStatus);
            logger.debug("Successfully marked announcement ID: {} as read by user ID: {}", announcementId, userId);
        } else {
            logger.debug("Announcement ID: {} already marked as read by user ID: {}", announcementId, userId);
        }
    }

    /**
     * Check if an announcement has been read by a user.
     * 
     * @param announcementId the announcement ID
     * @param userId the user ID
     * @return true if read, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isAnnouncementReadByUser(Long announcementId, Long userId) {
        Announcement announcement = announcementRepository.findById(announcementId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        
        if (announcement == null || user == null) {
            return false;
        }
        
        return readStatusRepository.existsByAnnouncementAndUser(announcement, user);
    }

    /**
     * Get announcement statistics for admin dashboard.
     * 
     * @return statistics object with counts
     */
    @Transactional(readOnly = true)
    public AnnouncementStatistics getAnnouncementStatistics() {
        long totalAnnouncements = announcementRepository.count();
        long activeAnnouncements = announcementRepository.countByActiveTrue();
        
        return new AnnouncementStatistics(totalAnnouncements, activeAnnouncements);
    }

    /**
     * Search announcements by title or content.
     * 
     * @param searchTerm the search term
     * @param activeOnly whether to search only active announcements
     * @param pageable pagination information
     * @param userId the ID of the requesting user (for read status)
     * @return page of matching announcements
     */
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> searchAnnouncements(String searchTerm, boolean activeOnly, 
                                                         Pageable pageable, Long userId) {
        logger.debug("Searching announcements with term: {} (activeOnly: {}) for user ID: {}", 
                    searchTerm, activeOnly, userId);

        Page<Announcement> announcements = announcementRepository.searchByTitleOrContent(
                searchTerm, activeOnly, pageable);
        
        // Get read status for the user
        Set<Long> readAnnouncementIds = null;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                readAnnouncementIds = readStatusRepository.findReadAnnouncementIdsByUser(user)
                        .stream().collect(Collectors.toSet());
            }
        }

        final Set<Long> finalReadIds = readAnnouncementIds;
        List<AnnouncementResponse> responses = announcements.getContent().stream()
                .map(announcement -> convertToResponse(announcement, finalReadIds))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, announcements.getTotalElements());
    }

    /**
     * Convert Announcement entity to AnnouncementResponse DTO.
     * 
     * @param announcement the announcement entity
     * @param readAnnouncementIds set of read announcement IDs for the user
     * @return the announcement response DTO
     */
    private AnnouncementResponse convertToResponse(Announcement announcement, Set<Long> readAnnouncementIds) {
        AnnouncementResponse response = new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getCreatorName(),
                announcement.getCreatedBy().getId(),
                announcement.getActive(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                announcement.getTargetDepartment()
        );

        // Set read status if available
        if (readAnnouncementIds != null) {
            response.setIsRead(readAnnouncementIds.contains(announcement.getId()));
        }

        return response;
    }

    /**
     * Inner class for announcement statistics.
     */
    public static class AnnouncementStatistics {
        private final long totalAnnouncements;
        private final long activeAnnouncements;

        public AnnouncementStatistics(long totalAnnouncements, long activeAnnouncements) {
            this.totalAnnouncements = totalAnnouncements;
            this.activeAnnouncements = activeAnnouncements;
        }

        public long getTotalAnnouncements() {
            return totalAnnouncements;
        }

        public long getActiveAnnouncements() {
            return activeAnnouncements;
        }

        public long getInactiveAnnouncements() {
            return totalAnnouncements - activeAnnouncements;
        }
    }
}
