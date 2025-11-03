package com.company.employeeportal.controller;

import com.company.employeeportal.dto.ChangePasswordRequest;
import com.company.employeeportal.dto.UserRequest;
import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.security.UserPrincipal;
import com.company.employeeportal.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testEmployee;
    private User testAdmin;
    private UserPrincipal employeePrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setName("John Doe");
        testEmployee.setEmail("john.doe@array.world");
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setDepartment("IT");
        testEmployee.setPosition("Developer");
        testEmployee.setContactInfo("123-456-7890");
        testEmployee.setJoinDate(LocalDate.of(2023, 1, 15));
        testEmployee.setActive(true);
        testEmployee.setCreatedAt(LocalDateTime.now());
        testEmployee.setUpdatedAt(LocalDateTime.now());

        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setName("Admin User");
        testAdmin.setEmail("admin@array.world");
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setActive(true);
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin.setUpdatedAt(LocalDateTime.now());

        employeePrincipal = new UserPrincipal(testEmployee);
        adminPrincipal = new UserPrincipal(testAdmin);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getCurrentUserProfile_WhenAuthenticated_ShouldReturnProfile() throws Exception {
        // Given
        when(userService.getUserProfile(1L)).thenReturn(testEmployee);

        // When & Then
        mockMvc.perform(get("/api/users/profile")
                .with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@array.world"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.department").value("IT"))
                .andExpect(jsonPath("$.position").value("Developer"));

        verify(userService).getUserProfile(1L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateCurrentUserProfile_WhenValidData_ShouldUpdateProfile() throws Exception {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("John Updated");
        userRequest.setContactInfo("987-654-3210");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("John Updated");
        updatedUser.setEmail("john.doe@array.world");
        updatedUser.setRole(Role.EMPLOYEE);
        updatedUser.setContactInfo("987-654-3210");
        updatedUser.setActive(true);

        when(userService.getUserProfile(1L)).thenReturn(testEmployee);
        when(userService.updateUserProfile(eq(1L), any(User.class), eq(testEmployee)))
                .thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/profile")
                .with(user(employeePrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.contactInfo").value("987-654-3210"));

        verify(userService).getUserProfile(1L);
        verify(userService).updateUserProfile(eq(1L), any(User.class), eq(testEmployee));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void changePassword_WhenValidRequest_ShouldChangePassword() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPassword");
        request.setNewPassword("NewPassword123!");
        request.setConfirmPassword("NewPassword123!");

        when(userService.getUserProfile(1L)).thenReturn(testEmployee);
        doNothing().when(userService).changePassword(1L, "currentPassword", "NewPassword123!", testEmployee);

        // When & Then
        mockMvc.perform(post("/api/users/profile/change-password")
                .with(user(employeePrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));

        verify(userService).getUserProfile(1L);
        verify(userService).changePassword(1L, "currentPassword", "NewPassword123!", testEmployee);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_WhenAdmin_ShouldReturnPagedUsers() throws Exception {
        // Given
        List<User> users = Arrays.asList(testEmployee, testAdmin);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), users.size());

        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        when(userService.getUsersWithPagination(any(), eq(testAdmin))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/users")
                .with(user(adminPrincipal))
                .param("page", "0")
                .param("size", "20")
                .param("sort", "name")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(userService).getUserProfile(2L);
        verify(userService).getUsersWithPagination(any(), eq(testAdmin));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAllUsers_WhenNotAdmin_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users")
                .with(user(employeePrincipal)))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUsersWithPagination(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUsers_WhenAdmin_ShouldReturnMatchingUsers() throws Exception {
        // Given
        List<User> users = Arrays.asList(testEmployee);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), users.size());

        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        when(userService.searchUsers(eq("John"), any(), eq(testAdmin))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/users/search")
                .with(user(adminPrincipal))
                .param("searchTerm", "John")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("John Doe"));

        verify(userService).getUserProfile(2L);
        verify(userService).searchUsers(eq("John"), any(), eq(testAdmin));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersByRole_WhenAdmin_ShouldReturnUsersByRole() throws Exception {
        // Given
        List<User> employees = Arrays.asList(testEmployee);

        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        when(userService.getUsersByRole(Role.EMPLOYEE, testAdmin)).thenReturn(employees);

        // When & Then
        mockMvc.perform(get("/api/users/by-role/EMPLOYEE")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("EMPLOYEE"));

        verify(userService).getUserProfile(2L);
        verify(userService).getUsersByRole(Role.EMPLOYEE, testAdmin);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_WhenAdmin_ShouldReturnUser() throws Exception {
        // Given
        when(userService.getUserProfile(1L)).thenReturn(testEmployee);

        // When & Then
        mockMvc.perform(get("/api/users/1")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@array.world"));

        verify(userService).getUserProfile(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_WhenAdmin_ShouldCreateUser() throws Exception {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("New User");
        userRequest.setEmail("new.user@array.world");
        userRequest.setPassword("Password123!");
        userRequest.setRole(Role.EMPLOYEE);
        userRequest.setDepartment("HR");
        userRequest.setPosition("Specialist");

        User createdUser = new User();
        createdUser.setId(3L);
        createdUser.setName("New User");
        createdUser.setEmail("new.user@array.world");
        createdUser.setRole(Role.EMPLOYEE);
        createdUser.setDepartment("HR");
        createdUser.setPosition("Specialist");
        createdUser.setActive(true);

        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        when(userService.createUser(any(User.class), eq(testAdmin))).thenReturn(createdUser);

        // When & Then
        mockMvc.perform(post("/api/users")
                .with(user(adminPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("new.user@array.world"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        verify(userService).getUserProfile(2L);
        verify(userService).createUser(any(User.class), eq(testAdmin));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createUser_WhenNotAdmin_ShouldReturnForbidden() throws Exception {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("New User");
        userRequest.setEmail("new.user@array.world");
        userRequest.setPassword("Password123!");

        // When & Then
        mockMvc.perform(post("/api/users")
                .with(user(employeePrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WhenAdmin_ShouldUpdateUser() throws Exception {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName("Updated User");
        userRequest.setDepartment("Finance");
        userRequest.setPosition("Manager");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated User");
        updatedUser.setEmail("john.doe@array.world");
        updatedUser.setDepartment("Finance");
        updatedUser.setPosition("Manager");
        updatedUser.setRole(Role.EMPLOYEE);
        updatedUser.setActive(true);

        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        when(userService.updateUserByAdmin(eq(1L), any(User.class), eq(testAdmin)))
                .thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .with(user(adminPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.department").value("Finance"))
                .andExpect(jsonPath("$.position").value("Manager"));

        verify(userService).getUserProfile(2L);
        verify(userService).updateUserByAdmin(eq(1L), any(User.class), eq(testAdmin));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateUser_WhenAdmin_ShouldDeactivateUser() throws Exception {
        // Given
        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        doNothing().when(userService).deactivateUser(1L, testAdmin);

        // When & Then
        mockMvc.perform(delete("/api/users/1")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string("User deactivated successfully"));

        verify(userService).getUserProfile(2L);
        verify(userService).deactivateUser(1L, testAdmin);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reactivateUser_WhenAdmin_ShouldReactivateUser() throws Exception {
        // Given
        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        doNothing().when(userService).reactivateUser(1L, testAdmin);

        // When & Then
        mockMvc.perform(post("/api/users/1/reactivate")
                .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string("User reactivated successfully"));

        verify(userService).getUserProfile(2L);
        verify(userService).reactivateUser(1L, testAdmin);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resetUserPassword_WhenAdmin_ShouldResetPassword() throws Exception {
        // Given
        when(userService.getUserProfile(2L)).thenReturn(testAdmin);
        doNothing().when(userService).changePassword(1L, null, "NewPassword123!", testAdmin);

        // When & Then
        mockMvc.perform(post("/api/users/1/reset-password")
                .with(user(adminPrincipal))
                .param("newPassword", "NewPassword123!"))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successfully"));

        verify(userService).getUserProfile(2L);
        verify(userService).changePassword(1L, null, "NewPassword123!", testAdmin);
    }

    @Test
    void getCurrentUserProfile_WhenNotAuthenticated_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getUserProfile(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateCurrentUserProfile_WhenInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given
        UserRequest userRequest = new UserRequest();
        userRequest.setName(""); // Invalid: empty name
        userRequest.setEmail("invalid-email"); // Invalid: bad email format

        // When & Then
        mockMvc.perform(put("/api/users/profile")
                .with(user(employeePrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserProfile(any(), any(), any());
    }
}
