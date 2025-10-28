package com.company.employeeportal.security;

import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomUserDetailsService.
 * Tests user loading and authentication scenarios.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.EMPLOYEE);
        testUser.setActive(true);
    }

    @Test
    void testLoadUserByUsername_Success() {
        when(userRepository.findByEmailAndActive("test@example.com", true))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_EMPLOYEE")));
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        when(userRepository.findByEmailAndActive(anyString(), eq(true)))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> 
                userDetailsService.loadUserByUsername("nonexistent@example.com"));
    }

    @Test
    void testLoadUserByUsername_InactiveUser() {
        testUser.setActive(false);
        when(userRepository.findByEmailAndActive("test@example.com", true))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> 
                userDetailsService.loadUserByUsername("test@example.com"));
    }

    @Test
    void testLoadUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserById(1L);

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void testLoadUserById_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> 
                userDetailsService.loadUserById(999L));
    }

    @Test
    void testLoadUserById_InactiveUser() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(UsernameNotFoundException.class, () -> 
                userDetailsService.loadUserById(1L));
    }

    @Test
    void testManagerRole() {
        testUser.setRole(Role.MANAGER);
        when(userRepository.findByEmailAndActive("test@example.com", true))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
        UserPrincipal userPrincipal = (UserPrincipal) userDetails;

        assertTrue(userPrincipal.hasRole("MANAGER"));
        assertTrue(userPrincipal.hasRole("EMPLOYEE"));
        assertFalse(userPrincipal.hasRole("ADMIN"));
    }

    @Test
    void testAdminRole() {
        testUser.setRole(Role.ADMIN);
        when(userRepository.findByEmailAndActive("test@example.com", true))
                .thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
        UserPrincipal userPrincipal = (UserPrincipal) userDetails;

        assertTrue(userPrincipal.hasRole("ADMIN"));
        assertTrue(userPrincipal.hasRole("MANAGER"));
        assertTrue(userPrincipal.hasRole("EMPLOYEE"));
    }
}