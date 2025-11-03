package com.company.employeeportal.security;

import com.company.employeeportal.model.Role;
import com.company.employeeportal.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * UserPrincipal class that implements Spring Security UserDetails.
 * Wraps User entity and provides authentication and authorization information.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final Boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String name, String email, String password, 
                        Role role, Boolean active, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
        this.authorities = authorities;
    }

    /**
     * Create UserPrincipal from User entity
     */
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = mapRoleToAuthorities(user.getRole());

        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getActive(),
                authorities
        );
    }

    /**
     * Map user role to Spring Security authorities
     */
    private static List<GrantedAuthority> mapRoleToAuthorities(Role role) {
        switch (role) {
            case IT_ADMIN:
                return List.of(
                    new SimpleGrantedAuthority("ROLE_IT_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                );
            case HR:
                return List.of(
                    new SimpleGrantedAuthority("ROLE_HR"),
                    new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                );
            case FINANCE:
                return List.of(
                    new SimpleGrantedAuthority("ROLE_FINANCE"),
                    new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                );
            case EMPLOYEE:
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
            default:
                return Collections.emptyList();
        }
    }

    // UserDetails interface methods
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // Additional getters for application use
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }

    // Helper methods for role checking
    public boolean hasRole(String roleName) {
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + roleName));
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isManager() {
        return hasRole("MANAGER");
    }

    public boolean isEmployee() {
        return hasRole("EMPLOYEE");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
