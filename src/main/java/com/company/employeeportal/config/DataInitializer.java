package com.company.employeeportal.config;

import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Data initializer to create default users for testing and development.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeUsers();
    }

    private void initializeUsers() {
        logger.info("Initializing default users...");

        // Check if users already exist
        if (userRepository.count() > 0) {
            logger.info("Users already exist, skipping initialization");
            return;
        }

        try {
            // Create Employee User
            User employee = new User();
            employee.setName("Jane Employee");
            employee.setEmail("employee@company.com");
            employee.setPassword(passwordEncoder.encode("Employee123!"));
            employee.setRole(Role.EMPLOYEE);
            employee.setDepartment("Engineering");
            employee.setPosition("Software Developer");
            employee.setContactInfo("+1-555-0103");
            employee.setJoinDate(LocalDate.of(2022, 6, 1));
            employee.setActive(true);
            userRepository.save(employee);
            logger.info("Created employee user: {}", employee.getEmail());

            // Create HR User
            User hrUser = new User();
            hrUser.setName("Sarah HR");
            hrUser.setEmail("hr@company.com");
            hrUser.setPassword(passwordEncoder.encode("HR123!"));
            hrUser.setRole(Role.HR);
            hrUser.setDepartment("Human Resources");
            hrUser.setPosition("HR Manager");
            hrUser.setContactInfo("+1-555-0102");
            hrUser.setJoinDate(LocalDate.of(2021, 3, 15));
            hrUser.setActive(true);
            userRepository.save(hrUser);
            logger.info("Created HR user: {}", hrUser.getEmail());

            // Create IT Admin User
            User itAdmin = new User();
            itAdmin.setName("Mike IT Admin");
            itAdmin.setEmail("itadmin@company.com");
            itAdmin.setPassword(passwordEncoder.encode("ITAdmin123!"));
            itAdmin.setRole(Role.IT_ADMIN);
            itAdmin.setDepartment("Information Technology");
            itAdmin.setPosition("IT Administrator");
            itAdmin.setContactInfo("+1-555-0101");
            itAdmin.setJoinDate(LocalDate.of(2020, 1, 1));
            itAdmin.setActive(true);
            userRepository.save(itAdmin);
            logger.info("Created IT Admin user: {}", itAdmin.getEmail());

            // Create Finance User
            User financeUser = new User();
            financeUser.setName("Lisa Finance");
            financeUser.setEmail("finance@company.com");
            financeUser.setPassword(passwordEncoder.encode("Finance123!"));
            financeUser.setRole(Role.FINANCE);
            financeUser.setDepartment("Finance");
            financeUser.setPosition("Finance Manager");
            financeUser.setContactInfo("+1-555-0104");
            financeUser.setJoinDate(LocalDate.of(2021, 8, 10));
            financeUser.setActive(true);
            userRepository.save(financeUser);
            logger.info("Created Finance user: {}", financeUser.getEmail());

            // Create additional test employees
            createTestEmployee("Alice Johnson", "alice@company.com", "Engineering", "Senior Developer");
            createTestEmployee("Bob Smith", "bob@company.com", "Marketing", "Marketing Specialist");
            createTestEmployee("Carol Davis", "carol@company.com", "Finance", "Financial Analyst");

            logger.info("Successfully initialized {} users", userRepository.count());

        } catch (Exception e) {
            logger.error("Error initializing users: {}", e.getMessage(), e);
        }
    }

    private void createTestEmployee(String name, String email, String department, String position) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.EMPLOYEE);
        user.setDepartment(department);
        user.setPosition(position);
        user.setContactInfo("+1-555-" + String.format("%04d", (int)(Math.random() * 9999)));
        user.setJoinDate(LocalDate.now().minusMonths((long)(Math.random() * 24)));
        user.setActive(true);
        userRepository.save(user);
        logger.info("Created test employee: {}", user.getEmail());
    }
}