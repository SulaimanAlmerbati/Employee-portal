package com.company.employeeportal.repository;

import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides custom query methods for user management functionality.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address (used for authentication).
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by email and active status.
     */
    Optional<User> findByEmailAndActive(String email, Boolean active);

    /**
     * Check if email exists in the system.
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users.
     */
    List<User> findByActiveTrue();

    /**
     * Find users by role.
     */
    List<User> findByRole(Role role);

    /**
     * Find users by multiple roles.
     */
    List<User> findByRoleIn(List<Role> roles);

    /**
     * Find active users by role.
     */
    List<User> findByRoleAndActiveTrue(Role role);

    /**
     * Find active users by multiple roles.
     */
    List<User> findByRoleInAndActiveTrue(List<Role> roles);

    /**
     * Find users by department.
     */
    List<User> findByDepartment(String department);

    /**
     * Find active users by department.
     */
    List<User> findByDepartmentAndActiveTrue(String department);

    /**
     * Find users with pagination and filtering by active status.
     */
    Page<User> findByActive(Boolean active, Pageable pageable);

    /**
     * Search users by name or email containing the search term.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "u.active = :active")
    Page<User> searchByNameOrEmail(@Param("searchTerm") String searchTerm, 
                                   @Param("active") Boolean active, 
                                   Pageable pageable);

    /**
     * Count users by role.
     */
    long countByRole(Role role);

    /**
     * Count active users by role.
     */
    long countByRoleAndActiveTrue(Role role);

    /**
     * Count total active users.
     */
    long countByActiveTrue();

    /**
     * Find managers and admins (users who can approve leaves).
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('MANAGER', 'ADMIN') AND u.active = true")
    List<User> findManagersAndAdmins();

    /**
     * Find users by department with pagination.
     */
    Page<User> findByDepartmentAndActiveTrue(String department, Pageable pageable);

    /**
     * Find users with advanced filtering.
     * Supports filtering by search term (name/email), role, department, and active status.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:department IS NULL OR :department = '' OR u.department = :department) AND " +
           "(:active IS NULL OR u.active = :active)")
    Page<User> findUsersWithFilters(@Param("search") String search,
                                    @Param("role") Role role,
                                    @Param("department") String department,
                                    @Param("active") Boolean active,
                                    Pageable pageable);
}