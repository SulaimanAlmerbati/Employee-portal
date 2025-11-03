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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnnouncementService.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementReadStatusRepository readStatusRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private User testAdmin;
    private User testEmployee;
    private Announcement testAnnouncement;
    private AnnouncementRequest testRequest;

    @BeforeEach
    void setUp() {
        // Create test admin user
        testAdmin = new User();
        testAdmin.setId(1L);
        testAdmin.setName("Admin User");
        testAdmin.setEmail("admin@array.world");
        testAdmin.setRole(Role.ADMIN);

        // Create test employee user
        testEmployee = new User();
        testEmployee.setId(2L);
        testEmployee.setName("Employee User");
        testEmployee.setEmail("employee@array.world");
        testEmployee.setRole(Role.EMPLOYEE);

        // Create test announcement
        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("Test Announcement");
        testAnnouncement.setContent("This is a test announcement content.");
        testAnnouncement.setCreatedBy(testAdmin);
        testAnnouncement.setActive(true);
        testAnnouncement.setCreatedAt(LocalDateTime.now());
        testAnnouncement.setUpdatedAt(LocalDateTime.now());

        // Create test request
        testRequest = new AnnouncementRequest();
        testRequest.setTitle("New Announcement");
        testRequest.setContent("New announcement content.");
    }

    @Test
    void createAnnouncement_WithAdminUser_ShouldCreateSuccessfully() {
        // Arrange
        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        // Act
        AnnouncementResponse response = announcementService.createAnnouncement(testRequest, testAdmin.getId());

        // Assert
        assertNotNull(response);
        assertEquals(testAnnouncement.getTitle(), response.getTitle());
        assertEquals(testAnnouncement.getContent(), response.getContent());
        assertEquals(testAdmin.getName(), response.getCreatedByName());
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    void createAnnouncement_WithNonAdminUser_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, 
                () -> announcementService.createAnnouncement(testRequest, testEmployee.getId()));
        verify(announcementRepository, never()).save(any(Announcement.class));
    }

    @Test
    void createAnnouncement_WithNonExistentUser_ShouldThrowUserNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, 
                () -> announcementService.createAnnouncement(testRequest, 999L));
        verify(announcementRepository, never()).save(any(Announcement.class));
    }

    @Test
    void getActiveAnnouncements_ShouldReturnPagedResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        Page<Announcement> announcementPage = new PageImpl<>(announcements, pageable, 1);
        
        when(announcementRepository.findByActiveTrueOrderByCreatedAtDesc(pageable))
                .thenReturn(announcementPage);
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.findReadAnnouncementIdsByUser(testEmployee)).thenReturn(Arrays.asList());

        // Act
        Page<AnnouncementResponse> result = announcementService.getActiveAnnouncements(pageable, testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testAnnouncement.getTitle(), result.getContent().get(0).getTitle());
        assertFalse(result.getContent().get(0).getIsRead());
    }

    @Test
    void getAllAnnouncements_WithAdminUser_ShouldReturnAllAnnouncements() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        Page<Announcement> announcementPage = new PageImpl<>(announcements, pageable, 1);
        
        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(announcementRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(announcementPage);

        // Act
        Page<AnnouncementResponse> result = announcementService.getAllAnnouncements(pageable, testAdmin.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testAnnouncement.getTitle(), result.getContent().get(0).getTitle());
    }

    @Test
    void getAllAnnouncements_WithNonAdminUser_ShouldThrowUnauthorizedException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, 
                () -> announcementService.getAllAnnouncements(pageable, testEmployee.getId()));
    }

    @Test
    void getRecentAnnouncements_ShouldReturnLimitedResults() {
        // Arrange
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        when(announcementRepository.findTopRecentActiveAnnouncements(5)).thenReturn(announcements);
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.findReadAnnouncementIdsByUser(testEmployee)).thenReturn(Arrays.asList());

        // Act
        List<AnnouncementResponse> result = announcementService.getRecentAnnouncements(testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAnnouncement.getTitle(), result.get(0).getTitle());
    }

    @Test
    void getAnnouncementById_WithValidId_ShouldReturnAnnouncement() {
        // Arrange
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.findReadAnnouncementIdsByUser(testEmployee)).thenReturn(Arrays.asList());

        // Act
        AnnouncementResponse result = announcementService.getAnnouncementById(testAnnouncement.getId(), testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testAnnouncement.getTitle(), result.getTitle());
        assertEquals(testAnnouncement.getContent(), result.getContent());
    }

    @Test
    void getAnnouncementById_WithInvalidId_ShouldThrowNotFoundException() {
        // Arrange
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AnnouncementNotFoundException.class, 
                () -> announcementService.getAnnouncementById(999L, testEmployee.getId()));
    }

    @Test
    void updateAnnouncement_WithAdminUser_ShouldUpdateSuccessfully() {
        // Arrange
        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        // Act
        AnnouncementResponse result = announcementService.updateAnnouncement(
                testAnnouncement.getId(), testRequest, testAdmin.getId());

        // Assert
        assertNotNull(result);
        verify(announcementRepository).save(testAnnouncement);
        assertEquals(testRequest.getTitle(), testAnnouncement.getTitle());
        assertEquals(testRequest.getContent(), testAnnouncement.getContent());
    }

    @Test
    void updateAnnouncement_WithNonAdminUser_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, 
                () -> announcementService.updateAnnouncement(testAnnouncement.getId(), testRequest, testEmployee.getId()));
        verify(announcementRepository, never()).save(any(Announcement.class));
    }

    @Test
    void deactivateAnnouncement_WithAdminUser_ShouldDeactivateSuccessfully() {
        // Arrange
        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        // Act
        announcementService.deactivateAnnouncement(testAnnouncement.getId(), testAdmin.getId());

        // Assert
        verify(announcementRepository).save(testAnnouncement);
        assertFalse(testAnnouncement.getActive());
    }

    @Test
    void markAnnouncementAsRead_ShouldCreateReadStatus() {
        // Arrange
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.existsByAnnouncementAndUser(testAnnouncement, testEmployee)).thenReturn(false);

        // Act
        announcementService.markAnnouncementAsRead(testAnnouncement.getId(), testEmployee.getId());

        // Assert
        verify(readStatusRepository).save(any(AnnouncementReadStatus.class));
    }

    @Test
    void markAnnouncementAsRead_WhenAlreadyRead_ShouldNotCreateDuplicate() {
        // Arrange
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.existsByAnnouncementAndUser(testAnnouncement, testEmployee)).thenReturn(true);

        // Act
        announcementService.markAnnouncementAsRead(testAnnouncement.getId(), testEmployee.getId());

        // Assert
        verify(readStatusRepository, never()).save(any(AnnouncementReadStatus.class));
    }

    @Test
    void isAnnouncementReadByUser_WhenRead_ShouldReturnTrue() {
        // Arrange
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.existsByAnnouncementAndUser(testAnnouncement, testEmployee)).thenReturn(true);

        // Act
        boolean result = announcementService.isAnnouncementReadByUser(testAnnouncement.getId(), testEmployee.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void isAnnouncementReadByUser_WhenNotRead_ShouldReturnFalse() {
        // Arrange
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.existsByAnnouncementAndUser(testAnnouncement, testEmployee)).thenReturn(false);

        // Act
        boolean result = announcementService.isAnnouncementReadByUser(testAnnouncement.getId(), testEmployee.getId());

        // Assert
        assertFalse(result);
    }

    @Test
    void getAnnouncementStatistics_ShouldReturnCorrectCounts() {
        // Arrange
        when(announcementRepository.count()).thenReturn(10L);
        when(announcementRepository.countByActiveTrue()).thenReturn(8L);

        // Act
        AnnouncementService.AnnouncementStatistics stats = announcementService.getAnnouncementStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals(10L, stats.getTotalAnnouncements());
        assertEquals(8L, stats.getActiveAnnouncements());
        assertEquals(2L, stats.getInactiveAnnouncements());
    }

    @Test
    void searchAnnouncements_ShouldReturnMatchingResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        Page<Announcement> announcementPage = new PageImpl<>(announcements, pageable, 1);
        
        when(announcementRepository.searchByTitleOrContent("test", true, pageable))
                .thenReturn(announcementPage);
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.findReadAnnouncementIdsByUser(testEmployee)).thenReturn(Arrays.asList());

        // Act
        Page<AnnouncementResponse> result = announcementService.searchAnnouncements(
                "test", true, pageable, testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testAnnouncement.getTitle(), result.getContent().get(0).getTitle());
    }

    @Test
    void getActiveAnnouncements_WithPagination_ShouldRespectPageSize() {
        // Arrange
        Pageable pageable = PageRequest.of(1, 5);
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        Page<Announcement> announcementPage = new PageImpl<>(announcements, pageable, 10);
        
        when(announcementRepository.findByActiveTrueOrderByCreatedAtDesc(pageable))
                .thenReturn(announcementPage);
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.findReadAnnouncementIdsByUser(testEmployee)).thenReturn(Arrays.asList());

        // Act
        Page<AnnouncementResponse> result = announcementService.getActiveAnnouncements(pageable, testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getNumberOfElements());
        assertEquals(5, result.getSize());
        assertEquals(1, result.getNumber());
        assertEquals(10, result.getTotalElements());
    }

    @Test
    void getAllAnnouncements_WithSorting_ShouldMaintainSortOrder() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        
        Announcement secondAnnouncement = new Announcement();
        secondAnnouncement.setId(2L);
        secondAnnouncement.setTitle("Second Announcement");
        secondAnnouncement.setContent("Second content");
        secondAnnouncement.setCreatedBy(testAdmin);
        secondAnnouncement.setActive(true);
        secondAnnouncement.setCreatedAt(LocalDateTime.now().minusHours(1));
        
        List<Announcement> announcements = Arrays.asList(testAnnouncement, secondAnnouncement);
        Page<Announcement> announcementPage = new PageImpl<>(announcements, pageable, 2);
        
        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(announcementRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(announcementPage);

        // Act
        Page<AnnouncementResponse> result = announcementService.getAllAnnouncements(pageable, testAdmin.getId());

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        // Verify that the first announcement is the more recent one
        assertEquals(testAnnouncement.getTitle(), result.getContent().get(0).getTitle());
    }

    @Test
    void createAnnouncement_ShouldMakeAnnouncementVisibleToAllEmployees() {
        // Arrange - Testing requirement 5.3 and 6.3
        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        // Act
        AnnouncementResponse response = announcementService.createAnnouncement(testRequest, testAdmin.getId());

        // Assert
        assertNotNull(response);
        assertTrue(response.getActive()); // Announcement should be active by default
        assertEquals(testAdmin.getName(), response.getCreatedByName());
        verify(announcementRepository).save(argThat(announcement -> 
            announcement.getActive() && 
            announcement.getCreatedBy().equals(testAdmin) &&
            announcement.getTitle().equals(testRequest.getTitle())
        ));
    }

    @Test
    void reactivateAnnouncement_WithAdminUser_ShouldReactivateSuccessfully() {
        // Arrange
        testAnnouncement.setActive(false); // Start with inactive announcement
        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(announcementRepository.findById(testAnnouncement.getId())).thenReturn(Optional.of(testAnnouncement));
        when(announcementRepository.save(any(Announcement.class))).thenReturn(testAnnouncement);

        // Act
        announcementService.reactivateAnnouncement(testAnnouncement.getId(), testAdmin.getId());

        // Assert
        verify(announcementRepository).save(testAnnouncement);
        assertTrue(testAnnouncement.getActive());
    }

    @Test
    void reactivateAnnouncement_WithNonAdminUser_ShouldThrowUnauthorizedException() {
        // Arrange
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, 
                () -> announcementService.reactivateAnnouncement(testAnnouncement.getId(), testEmployee.getId()));
        verify(announcementRepository, never()).save(any(Announcement.class));
    }

    @Test
    void searchAnnouncements_WithInactiveOnly_ShouldReturnInactiveAnnouncements() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        testAnnouncement.setActive(false);
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        Page<Announcement> announcementPage = new PageImpl<>(announcements, pageable, 1);
        
        when(announcementRepository.searchByTitleOrContent("test", false, pageable))
                .thenReturn(announcementPage);
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.findReadAnnouncementIdsByUser(testEmployee)).thenReturn(Arrays.asList());

        // Act
        Page<AnnouncementResponse> result = announcementService.searchAnnouncements(
                "test", false, pageable, testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).getActive());
    }

    @Test
    void getActiveAnnouncements_WithReadStatus_ShouldMarkReadAnnouncements() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        Page<Announcement> announcementPage = new PageImpl<>(announcements, pageable, 1);
        
        when(announcementRepository.findByActiveTrueOrderByCreatedAtDesc(pageable))
                .thenReturn(announcementPage);
        when(userRepository.findById(testEmployee.getId())).thenReturn(Optional.of(testEmployee));
        when(readStatusRepository.findReadAnnouncementIdsByUser(testEmployee))
                .thenReturn(Arrays.asList(testAnnouncement.getId()));

        // Act
        Page<AnnouncementResponse> result = announcementService.getActiveAnnouncements(pageable, testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getIsRead());
    }
}
