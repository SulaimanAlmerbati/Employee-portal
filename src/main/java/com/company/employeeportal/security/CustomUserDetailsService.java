package com.company.employeeportal.security;

import com.company.employeeportal.model.User;
import com.company.employeeportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation that loads user details from the database.
 * Maps User entity to Spring Security UserDetails for authentication.
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load user details by email (username) for authentication.
     * Only loads active users for security purposes.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndActive(email, true)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found with email: " + email));

        return UserPrincipal.create(user);
    }

    /**
     * Load user details by user ID.
     * Used for token refresh and other operations requiring user lookup by ID.
     */
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        User user = userRepository.findById(id)
                .filter(User::getActive)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found with id: " + id));

        return UserPrincipal.create(user);
    }
}
