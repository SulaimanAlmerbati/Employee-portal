package com.company.employeeportal.integration;

import com.company.employeeportal.dto.AnnouncementRequest;
import com.company.employeeportal.dto.AnnouncementResponse;
import com.company.employeeportal.model.Announcement;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.AnnouncementRepository;
import com.company.employeeportal.repository.UserRepository;
import com.company.employeeportal.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for announcement functionality covering:
 * - Announcement creation and management (Requirement 5.3, 6.3)
 * - Role-based access control
 * - Pagination and sorting functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnnouncementIntegrationTest {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User employeeUser;
    private User managerUser;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        announcementRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        adminUser = createUser("admin@company.com", "Admin User", Role.ADMIN);
        employeeUser = createUser("employee@company.com", "Employee User", Role.EMPLOYEE);
        managerUser = createUser("manager@company.com", "Manager User", Role.MANAGER);

        // Create test announcements
        createTestAnnouncements();
    }

    private User createUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword("password123");
        user.setRole(role);
        user.setDepartment("IT");
        user.setJoinDate(LocalDate.now());
        user.setActive(true);
        return userRepository.save(user);
    }

    private void createTestAnnouncements() {
        // Create multiple announcements for pagination testing
        for (int i = 1; i <= 15; i++) {
            Announcement announcement = new Announcement();
            announcement.setTitle("Test Announcement " + i);
            announcement.setContent("This is test announcement content number " + i);
            announcement.setCreatedBy(adminUser);
            announcement.setActive(i <= 12); // Make some inactive for testing
            announcement.setCreatedAt(LocalDateTime.now().minusDays(i));
            announcementRepository.save(announcement);
        }
    }

    @Test
    void testAnnouncementCreationMakesVisibleToAllEmployees() {
        // Arrange - Testing Requirements 5.3 and 6.3
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("Company Policy Update");
        request.setContent("Important policy changes effective immediately.");

        // Act - Admin creates announcement
        AnnouncementResponse response = announcementService.createAnnouncement(request, adminUser.getId());

        // Assert - Announcement should be visible to all employees
        assertNotNull(response);
        assertTrue(response.getActive());
        assertEquals(adminUser.getName(), response.getCreatedByName());

        // Verify employees can see the announcement
        Page<AnnouncementResponse> employeeView = announcementService.getActiveAnnouncements(
                PageRequest.of(0, 10), employeeUser.getId());
        
        assertTrue(employeeView.getContent().stream()
                .anyMatch(ann -> ann.getTitle().equals("Company Policy Update")));

        // Verify managers can also see the announcement
        Page<AnnouncementResponse> managerView = announcementService.getActiveAnnouncements(
                PageRequest.of(0, 10), managerUser.getId());
        
        assertTrue(managerView.getContent().stream()
                .anyMatch(ann -> ann.getTitle().equals("Company Policy Update")));
    }

    @Test
    void testRoleBasedAccessControl_AdminCanManageAnnouncements() {
        // Test admin can create announcements
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("Admin Announcement");
        request.setContent("Admin created announcement");

        AnnouncementResponse response = announcementService.createAnnouncement(request, adminUser.getId());
        assertNotNull(response);
        assertEquals("Admin Announcement", response.getTitle());
        assertEquals(adminUser.getName(), response.getCreatedByName());

        // Test admin can view all announcements
        Page<AnnouncementResponse> allAnnouncements = announcementService.getAllAnnouncements(
                PageRequest.of(0, 20), adminUser.getId());
        assertNotNull(allAnnouncements);
        assertTrue(allAnnouncements.getTotalElements() > 0);

        // Test admin can get statistics
        AnnouncementService.AnnouncementStatistics stats = announcementService.getAnnouncementStatistics();
        assertNotNull(stats);
        assertTrue(stats.getTotalAnnouncements() > 0);
    }

    @Test
    void testRoleBasedAccessControl_EmployeeCannotManageAnnouncements() {
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("Employee Attempt");
        request.setContent("Employee trying to create announcement");

        // Test employee cannot create announcements
        assertThrows(Exception.class, () -> 
                announcementService.createAnnouncement(request, employeeUser.getId()));

        // Test employee cannot access admin functions
        assertThrows(Exception.class, () -> 
                announcementService.getAllAnnouncements(PageRequest.of(0, 10), employeeUser.getId()));
    }

    @Test
    void testRoleBasedAccessControl_ManagerCannotManageAnnouncements() {
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("Manager Attempt");
        request.setContent("Manager trying to create announcement");

        // Test manager cannot create announcements
        assertThrows(Exception.class, () -> 
                announcementService.createAnnouncement(request, managerUser.getId()));

        // Test manager cannot access admin functions
        assertThrows(Exception.class, () -> 
                announcementService.getAllAnnouncements(PageRequest.of(0, 10), managerUser.getId()));
    }

    @Test
    void testPaginationFunctionality() {
        // Test first page with page size 5
        Page<AnnouncementResponse> firstPage = announcementService.getActiveAnnouncements(
                PageRequest.of(0, 5), employeeUser.getId());
        
        assertNotNull(firstPage);
        assertEquals(5, firstPage.getSize());
        assertEquals(0, firstPage.getNumber());
        assertEquals(12, firstPage.getTotalElements()); // Only active announcements
        assertEquals(3, firstPage.getTotalPages());

        // Test second page
        Page<AnnouncementResponse> secondPage = announcementService.getActiveAnnouncements(
                PageRequest.of(1, 5), employeeUser.getId());
        
        assertEquals(1, secondPage.getNumber());
        assertEquals(5, secondPage.getContent().size());

        // Test last page
        Page<AnnouncementResponse> lastPage = announcementService.getActiveAnnouncements(
                PageRequest.of(2, 5), employeeUser.getId());
        
        assertEquals(2, lastPage.getNumber());
        assertEquals(2, lastPage.getContent().size()); // Last page has 2 items
    }

    @Test
    void testSortingFunctionality() {
        // Test sorting by creation date descending (default behavior)
        Page<AnnouncementResponse> sortedPage = announcementService.getActiveAnnouncements(
                PageRequest.of(0, 15), employeeUser.getId());
        
        assertNotNull(sortedPage);
        List<AnnouncementResponse> content = sortedPage.getContent();
        assertTrue(content.size() > 1);
        
        // Verify announcements are sorted by creation date descending
        for (int i = 0; i < content.size() - 1; i++) {
            assertTrue(content.get(i).getCreatedAt().isAfter(content.get(i + 1).getCreatedAt()) ||
                      content.get(i).getCreatedAt().isEqual(content.get(i + 1).getCreatedAt()));
        }
    }

    @Test
    void testSearchWithPaginationAndSorting() {
        // Test search with pagination
        Page<AnnouncementResponse> searchResults = announcementService.searchAnnouncements(
                "Test", true, PageRequest.of(0, 3), employeeUser.getId());
        
        assertNotNull(searchResults);
        assertEquals(3, searchResults.getSize());
        assertEquals(0, searchResults.getNumber());
        assertEquals(12, searchResults.getTotalElements()); // All active announcements match "Test"
        
        // Verify all results contain the search term
        searchResults.getContent().forEach(announcement -> 
                assertTrue(announcement.getTitle().contains("Test") || 
                          announcement.getContent().contains("Test")));
    }

    @Test
    void testAnnouncementReadStatusTracking() {
        // Test marking announcement as read
        List<Announcement> announcements = announcementRepository.findByActiveTrue();
        assertFalse(announcements.isEmpty());
        
        Long announcementId = announcements.get(0).getId();
        
        // Initially should be unread
        assertFalse(announcementService.isAnnouncementReadByUser(announcementId, employeeUser.getId()));
        
        // Mark as read
        announcementService.markAnnouncementAsRead(announcementId, employeeUser.getId());
        
        // Should now be read
        assertTrue(announcementService.isAnnouncementReadByUser(announcementId, employeeUser.getId()));
        
        // Verify read status appears in announcement list
        Page<AnnouncementResponse> announcements_page = announcementService.getActiveAnnouncements(
                PageRequest.of(0, 10), employeeUser.getId());
        
        AnnouncementResponse readAnnouncement = announcements_page.getContent().stream()
                .filter(ann -> ann.getId().equals(announcementId))
                .findFirst()
                .orElse(null);
        
        assertNotNull(readAnnouncement);
        assertTrue(readAnnouncement.getIsRead());
    }

    @Test
    void testAnnouncementStatistics() {
        // Test statistics calculation
        AnnouncementService.AnnouncementStatistics stats = announcementService.getAnnouncementStatistics();
        
        assertNotNull(stats);
        assertEquals(15L, stats.getTotalAnnouncements());
        assertEquals(12L, stats.getActiveAnnouncements());
        assertEquals(3L, stats.getInactiveAnnouncements());
    }

    @Test
    void testAnnouncementDeactivationAndReactivation() {
        // Create a new announcement
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("Test Deactivation");
        request.setContent("This announcement will be deactivated");
        
        AnnouncementResponse created = announcementService.createAnnouncement(request, adminUser.getId());
        assertTrue(created.getActive());
        
        // Deactivate the announcement
        announcementService.deactivateAnnouncement(created.getId(), adminUser.getId());
        
        // Verify it's no longer in active announcements
        Page<AnnouncementResponse> activeAnnouncements = announcementService.getActiveAnnouncements(
                PageRequest.of(0, 20), employeeUser.getId());
        
        assertFalse(activeAnnouncements.getContent().stream()
                .anyMatch(ann -> ann.getId().equals(created.getId())));
        
        // Reactivate the announcement
        announcementService.reactivateAnnouncement(created.getId(), adminUser.getId());
        
        // Verify it's back in active announcements
        activeAnnouncements = announcementService.getActiveAnnouncements(
                PageRequest.of(0, 20), employeeUser.getId());
        
        assertTrue(activeAnnouncements.getContent().stream()
                .anyMatch(ann -> ann.getId().equals(created.getId())));
    }
}