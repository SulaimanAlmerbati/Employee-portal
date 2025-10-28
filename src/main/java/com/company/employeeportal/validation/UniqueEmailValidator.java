package com.company.employeeportal.validation;

import com.company.employeeportal.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validator for UniqueEmail annotation.
 * Checks if email already exists in the database.
 */
@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;
    private long excludeUserId;

    @Autowired
    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {
        this.excludeUserId = constraintAnnotation.excludeUserId();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Let @NotBlank handle null/empty validation
        }

        // Check if email exists
        boolean emailExists = userRepository.existsByEmail(email.toLowerCase().trim());
        
        if (!emailExists) {
            return true; // Email is unique
        }

        // If excludeUserId is provided, check if the existing email belongs to that user
        if (excludeUserId > 0) {
            return userRepository.findByEmail(email.toLowerCase().trim())
                    .map(user -> user.getId().equals(excludeUserId))
                    .orElse(false);
        }

        return false; // Email already exists
    }
}