package com.company.employeeportal.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnnouncementTest {

    private Validator validator;
    private Announcement announcement;
    private User creator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        creator = new User();
        creator.setId(1L);
        creator.setName("Admin User");
        creator.setEmail("admin@company.com");
        creator.setRole(Role.ADMIN);
        
        announcement = new Announcement();
        announcement.setTitle("Important Announcement");
        announcement.setContent("This is an important company announcement.");
        announcement.setCreatedBy(creator);
    }

    @Test
    void testValidAnnouncement() {
        Set<ConstraintViolation<Announcement>> violations = validator.validate(announcement);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBlankTitle() {
        announcement.setTitle("");
        Set<ConstraintViolation<Announcement>> violations = validator.validate(announcement);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title is required")));
    }

    @Test
    void testBlankContent() {
        announcement.setContent("");
        Set<ConstraintViolation<Announcement>> violations = validator.validate(announcement);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Content is required")));
    }

    @Test
    void testNullCreator() {
        announcement.setCreatedBy(null);
        Set<ConstraintViolation<Announcement>> violations = validator.validate(announcement);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Creator is required")));
    }

    @Test
    void testTitleTooLong() {
        String longTitle = "A".repeat(201);
        announcement.setTitle(longTitle);
        Set<ConstraintViolation<Announcement>> violations = validator.validate(announcement);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title must not exceed 200 characters")));
    }

    @Test
    void testDefaultActiveStatus() {
        Announcement newAnnouncement = new Announcement();
        assertTrue(newAnnouncement.getActive());
    }

    @Test
    void testActiveHelperMethods() {
        assertTrue(announcement.isActive());
        
        announcement.deactivate();
        assertFalse(announcement.isActive());
        
        announcement.activate();
        assertTrue(announcement.isActive());
    }

    @Test
    void testGetCreatorName() {
        assertEquals("Admin User", announcement.getCreatorName());
        
        announcement.setCreatedBy(null);
        assertEquals("Unknown", announcement.getCreatorName());
    }

    @Test
    void testContentPreview() {
        String shortContent = "Short content";
        announcement.setContent(shortContent);
        assertEquals(shortContent, announcement.getPreview());
        
        String longContent = "A".repeat(150);
        announcement.setContent(longContent);
        String preview = announcement.getPreview();
        assertEquals(103, preview.length()); // 100 chars + "..."
        assertTrue(preview.endsWith("..."));
    }

    @Test
    void testConstructorWithParameters() {
        Announcement newAnnouncement = new Announcement("Test Title", "Test Content", creator);
        assertEquals("Test Title", newAnnouncement.getTitle());
        assertEquals("Test Content", newAnnouncement.getContent());
        assertEquals(creator, newAnnouncement.getCreatedBy());
        assertTrue(newAnnouncement.getActive());
    }

    @Test
    void testEntityRelationships() {
        // Test createdBy relationship
        assertNotNull(announcement.getCreatedBy());
        assertEquals(creator, announcement.getCreatedBy());
    }

    @Test
    void testAuditFields() {
        // Test that audit fields are properly configured
        assertNull(announcement.getCreatedAt()); // Will be set by @CreationTimestamp
        assertNull(announcement.getUpdatedAt()); // Will be set by @UpdateTimestamp
    }

    @Test
    void testToString() {
        announcement.setId(1L);
        String toString = announcement.toString();
        assertTrue(toString.contains("Announcement{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("title='Important Announcement'"));
        assertTrue(toString.contains("active=true"));
        assertTrue(toString.contains("createdBy=Admin User"));
    }
}