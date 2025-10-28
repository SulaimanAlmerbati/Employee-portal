package com.company.employeeportal.repository;

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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User employee;
    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setName("John Employee");
        employee.setEmail("john@company.com");
        employee.setPassword("password123");
        employee.setRole(Role.EMPLOYEE);
        employee.setDepartment("IT");
        employee.setJoinDate(LocalDate.now());
        employee.setActive(true);

        manager = new User();
        manager.setName("Jane Manager");
        manager.setEmail("jane@company.com");
        manager.setPassword("password456");
        manager.setRole(Role.MANAGER);
        manager.setDepartment("HR");
        manager.setJoinDate(LocalDate.now());
        manager.setActive(true);

        admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@company.com");
        admin.setPassword("password789");
        admin.setRole(Role.ADMIN);
        admin.setDepartment("IT");
        admin.setJoinDate(LocalDate.now());
        admin.setActive(false);

        entityManager.persistAndFlush(employee);
        entityManager.persistAndFlush(manager);
        entityManager.persistAndFlush(admin);
    }

    @Test
    void testFindByEmail() {
        Optional<User> found = userRepository.findByEmail("john@company.com");
        assertTrue(found.isPresent());
        assertEquals("John Employee", found.get().getName());
    }

    @Test
    void testFindByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("notfound@company.com");
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByEmail() {
        assertTrue(userRepository.existsByEmail("john@company.com"));
        assertFalse(userRepository.existsByEmail("notfound@company.com"));
    }

    @Test
    void testFindByActiveTrue() {
        List<User> activeUsers = userRepository.findByActiveTrue();
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().allMatch(User::getActive));
    }

    @Test
    void testFindByRole() {
        List<User> employees = userRepository.findByRole(Role.EMPLOYEE);
        assertEquals(1, employees.size());
        assertEquals("John Employee", employees.get(0).getName());

        List<User> managers = userRepository.findByRole(Role.MANAGER);
        assertEquals(1, managers.size());
        assertEquals("Jane Manager", managers.get(0).getName());
    }

    @Test
    void testFindByRoleAndActiveTrue() {
        List<User> activeEmployees = userRepository.findByRoleAndActiveTrue(Role.EMPLOYEE);
        assertEquals(1, activeEmployees.size());

        List<User> activeAdmins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);
        assertEquals(0, activeAdmins.size()); // Admin is inactive
    }

    @Test
    void testFindByDepartment() {
        List<User> itUsers = userRepository.findByDepartment("IT");
        assertEquals(2, itUsers.size());

        List<User> hrUsers = userRepository.findByDepartment("HR");
        assertEquals(1, hrUsers.size());
        assertEquals("Jane Manager", hrUsers.get(0).getName());
    }

    @Test
    void testCountByRole() {
        assertEquals(1, userRepository.countByRole(Role.EMPLOYEE));
        assertEquals(1, userRepository.countByRole(Role.MANAGER));
        assertEquals(1, userRepository.countByRole(Role.ADMIN));
    }

    @Test
    void testCountByActiveTrue() {
        assertEquals(2, userRepository.countByActiveTrue());
    }

    @Test
    void testFindManagersAndAdmins() {
        List<User> managersAndAdmins = userRepository.findManagersAndAdmins();
        assertEquals(1, managersAndAdmins.size()); // Only active manager
        assertEquals("Jane Manager", managersAndAdmins.get(0).getName());
    }

    @Test
    void testSearchByNameOrEmail() {
        Page<User> searchResults = userRepository.searchByNameOrEmail("john", true, 
                                                                     PageRequest.of(0, 10));
        assertEquals(1, searchResults.getContent().size());
        assertEquals("John Employee", searchResults.getContent().get(0).getName());

        searchResults = userRepository.searchByNameOrEmail("company.com", true, 
                                                          PageRequest.of(0, 10));
        assertEquals(2, searchResults.getContent().size()); // Both active users have @company.com
    }

    @Test
    void testFindByEmailAndActive() {
        Optional<User> found = userRepository.findByEmailAndActive("john@company.com", true);
        assertTrue(found.isPresent());
        assertEquals("John Employee", found.get().getName());

        Optional<User> notFound = userRepository.findByEmailAndActive("admin@company.com", true);
        assertFalse(notFound.isPresent()); // Admin is inactive
    }

    @Test
    void testFindByDepartmentAndActiveTrue() {
        List<User> activeItUsers = userRepository.findByDepartmentAndActiveTrue("IT");
        assertEquals(1, activeItUsers.size()); // Only employee is active in IT
        assertEquals("John Employee", activeItUsers.get(0).getName());
    }

    @Test
    void testFindByActive() {
        Page<User> activeUsers = userRepository.findByActive(true, PageRequest.of(0, 10));
        assertEquals(2, activeUsers.getContent().size());

        Page<User> inactiveUsers = userRepository.findByActive(false, PageRequest.of(0, 10));
        assertEquals(1, inactiveUsers.getContent().size());
        assertEquals("Admin User", inactiveUsers.getContent().get(0).getName());
    }

    @Test
    void testFindByDepartmentAndActiveTrueWithPagination() {
        Page<User> hrUsers = userRepository.findByDepartmentAndActiveTrue("HR", PageRequest.of(0, 10));
        assertEquals(1, hrUsers.getContent().size());
        assertEquals("Jane Manager", hrUsers.getContent().get(0).getName());
    }
}