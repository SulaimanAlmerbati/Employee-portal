package com.company.employeeportal.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * Validator for ValidName annotation.
 * Validates that name contains only allowed characters.
 */
@Component
public class ValidNameValidator implements ConstraintValidator<ValidName, String> {

    private boolean allowEmpty;

    @Override
    public void initialize(ValidName constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null || name.trim().isEmpty()) {
            return allowEmpty; // Let @NotBlank handle null/empty validation if not allowed
        }

        // Allow letters (including Unicode letters), spaces, hyphens, and apostrophes
        // This pattern supports international names
        String namePattern = "^[\\p{L}\\s\\-']+$";
        
        if (!name.matches(namePattern)) {
            return false;
        }

        // Additional checks for reasonable name format
        String trimmedName = name.trim();
        
        // Name should not start or end with special characters
        if (trimmedName.startsWith("-") || trimmedName.startsWith("'") ||
            trimmedName.endsWith("-") || trimmedName.endsWith("'")) {
            return false;
        }

        // Name should not have consecutive special characters
        if (trimmedName.contains("--") || trimmedName.contains("''") || 
            trimmedName.contains("- ") || trimmedName.contains(" -") ||
            trimmedName.contains("' ") || trimmedName.contains(" '")) {
            return false;
        }

        // Name should not have excessive consecutive spaces
        if (trimmedName.contains("  ")) {
            return false;
        }

        return true;
    }
}