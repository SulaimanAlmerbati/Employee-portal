package com.company.employeeportal.repository;

import com.company.employeeportal.model.Announcement;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AnnouncementRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AnnouncementRepository announcementRepository;

    private User admin;
    private Announcement activeAnnouncement;
    private Announcement inactiveAnnouncement;
    private Announcement recentAnnouncement;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@company.com");
        admin.setPassword("password123");
        admin.setRole(Role.ADMIN);
        admin.setDepartment("IT");
        admin.setJoinDate(LocalDate.now());
        admin.setActive(true);

        entityManager.persistAndFlush(admin);

        activeAnnouncement = new Announcement();
        activeAnnouncement.setTitle("Company Holiday Schedule");
        activeAnnouncement.setContent("Please note the upcoming holiday schedule for December.");
        activeAnnouncement.setCreatedBy(admin);
        activeAnnouncement.setActive(true);

        inactiveAnnouncement = new Announcement();
        inactiveAnnouncement.setTitle("Old Policy Update");
        inactiveAnnouncement.setContent("This is an outdated policy announcement.");
        inactiveAnnouncement.setCreatedBy(admin);
        inactiveAnnouncement.setActive(false);

        recentAnnouncement = new Announcement();
        recentAnnouncement.setTitle("New Office Guidelines");
        recentAnnouncement.setContent("Updated guidelines for office safety and protocols.");
        recentAnnouncement.setCreatedBy(admin);
        recentAnnouncement.setActive(true);

        entityManager.persistAndFlush(activeAnnouncement);
        entityManager.persistAndFlush(inactiveAnnouncement);
        entityManager.persistAndFlush(recentAnnouncement);
    }

    @Test
    void testFindByActiveTrue() {
        List<Announcement> activeAnnouncements = announcementRepository.findByActiveTrue();
        assertEquals(2, activeAnnouncements.size());
        assertTrue(activeAnnouncements.stream().allMatch(Announcement::getActive));
    }

    @Test
    void testFindByActiveTrueWithPagination() {
        Page<Announcement> activePage = announcementRepository.findByActiveTrue(PageRequest.of(0, 1));
        assertEquals(1, activePage.getContent().size());
        assertEquals(2, activePage.getTotalElements());
        assertTrue(activePage.getContent().get(0).getActive());
    }

    @Test
    void testFindByActiveTrueOrderByCreatedAtDesc() {
        List<Announcement> announcements = announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
        assertEquals(2, announcements.size());
        assertTrue(announcements.stream().allMatch(Announcement::getActive));
        
        // Should be ordered by creation date descending
        assertTrue(announcements.get(0).getCreatedAt().isAfter(announcements.get(1).getCreatedAt()) ||
                  announcements.get(0).getCreatedAt().isEqual(announcements.get(1).getCreatedAt()));
    }

    @Test
    void testFindByActiveTrueOrderByCreatedAtDescWithPagination() {
        Page<Announcement> announcementPage = announcementRepository
            .findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, 10));
        
        assertEquals(2, announcementPage.getContent().size());
        assertTrue(announcementPage.getContent().stream().allMatch(Announcement::getActive));
    }

    @Test
    void testFindByCreatedBy() {
        List<Announcement> adminAnnouncements = announcementRepository.findByCreatedBy(admin);
        assertEquals(3, adminAnnouncements.size());
    }

    @Test
    void testFindByCreatedByAndActiveTrue() {
        List<Announcement> activeAdminAnnouncements = announcementRepository.findByCreatedByAndActiveTrue(admin);
        assertEquals(2, activeAdminAnnouncements.size());
        assertTrue(activeAdminAnnouncements.stream().allMatch(Announcement::getActive));
    }

    @Test
    void testFindByCreatedByWithPagination() {
        Page<Announcement> adminAnnouncementPage = announcementRepository
            .findByCreatedBy(admin, PageRequest.of(0, 2));
        
        assertEquals(2, adminAnnouncementPage.getContent().size());
        assertEquals(3, adminAnnouncementPage.getTotalElements());
    }

    @Test
    void testFindAllByOrderByCreatedAtDesc() {
        Page<Announcement> allAnnouncements = announcementRepository
            .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
        
        assertEquals(3, allAnnouncements.getContent().size());
    }

    @Test
    void testFindByTitleContainingIgnoreCase() {
        Page<Announcement> searchResults = announcementRepository
            .findByTitleContainingIgnoreCase("holiday", true, PageRequest.of(0, 10));
        
        assertEquals(1, searchResults.getContent().size());
        assertEquals("Company Holiday Schedule", searchResults.getContent().get(0).getTitle());
    }

    @Test
    void testSearchByTitleOrContent() {
        Page<Announcement> searchResults = announcementRepository
            .searchByTitleOrContent("guidelines", true, PageRequest.of(0, 10));
        
        assertEquals(1, searchResults.getContent().size());
        assertTrue(searchResults.getContent().get(0).getTitle().contains("Guidelines") ||
                  searchResults.getContent().get(0).getContent().contains("guidelines"));
    }

    @Test
    void testCountByActiveTrue() {
        long activeCount = announcementRepository.countByActiveTrue();
        assertEquals(2, activeCount);
    }

    @Test
    void testCount() {
        long totalCount = announcementRepository.count();
        assertEquals(3, totalCount);
    }

    @Test
    void testCountByCreatedBy() {
        long adminCount = announcementRepository.countByCreatedBy(admin);
        assertEquals(3, adminCount);
    }

    @Test
    void testCountByCreatedByAndActiveTrue() {
        long activeAdminCount = announcementRepository.countByCreatedByAndActiveTrue(admin);
        assertEquals(2, activeAdminCount);
    }

    @Test
    void testFindByCreatedAtBetween() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        
        List<Announcement> announcements = announcementRepository
            .findByCreatedAtBetween(startDate, endDate);
        
        assertEquals(3, announcements.size());
    }

    @Test
    void testFindActiveByCreatedAtBetween() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        
        List<Announcement> activeAnnouncements = announcementRepository
            .findActiveByCreatedAtBetween(startDate, endDate);
        
        assertEquals(2, activeAnnouncements.size());
        assertTrue(activeAnnouncements.stream().allMatch(Announcement::getActive));
    }

    @Test
    void testFindRecentActiveAnnouncements() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Announcement> recentAnnouncements = announcementRepository
            .findRecentActiveAnnouncements(thirtyDaysAgo);
        
        assertEquals(2, recentAnnouncements.size());
        assertTrue(recentAnnouncements.stream().allMatch(Announcement::getActive));
    }

    @Test
    void testFindTopRecentActiveAnnouncements() {
        List<Announcement> topAnnouncements = announcementRepository
            .findTopRecentActiveAnnouncements(1);
        
        assertEquals(1, topAnnouncements.size());
        assertTrue(topAnnouncements.get(0).getActive());
    }
}