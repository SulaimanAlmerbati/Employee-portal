package com.company.employeeportal.validation;

import com.company.employeeportal.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Validator for UniqueEmailForUpdate annotation.
 * Checks if email already exists in the database, excluding the current user.
 */
@Component
public class UniqueEmailForUpdateValidator implements ConstraintValidator<UniqueEmailForUpdate, Object> {

    private final UserRepository userRepository;
    private String emailField;
    private String userIdField;

    @Autowired
    public UniqueEmailForUpdateValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(UniqueEmailForUpdate constraintAnnotation) {
        this.emailField = constraintAnnotation.emailField();
        this.userIdField = constraintAnnotation.userIdField();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        try {
            String email = getFieldValue(obj, emailField);
            Long userId = getFieldValueAsLong(obj, userIdField);

            if (email == null || email.trim().isEmpty()) {
                return true; // Let @NotBlank handle null/empty validation
            }

            // Check if email exists
            boolean emailExists = userRepository.existsByEmail(email.toLowerCase().trim());
            
            if (!emailExists) {
                return true; // Email is unique
            }

            // If userId is provided, check if the existing email belongs to that user
            if (userId != null && userId > 0) {
                return userRepository.findByEmail(email.toLowerCase().trim())
                        .map(user -> user.getId().equals(userId))
                        .orElse(false);
            }

            return false; // Email already exists

        } catch (Exception e) {
            // If we can't access the fields, consider it invalid
            return false;
        }
    }

    private String getFieldValue(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(obj);
        return value != null ? value.toString() : null;
    }

    private Long getFieldValueAsLong(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(obj);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}