package com.company.employeeportal.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * Validator for ValidContactInfo annotation.
 * Validates contact information format (phone numbers, emails, etc.).
 */
@Component
public class ValidContactInfoValidator implements ConstraintValidator<ValidContactInfo, String> {

    private boolean allowEmpty;

    @Override
    public void initialize(ValidContactInfo constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String contactInfo, ConstraintValidatorContext context) {
        if (contactInfo == null || contactInfo.trim().isEmpty()) {
            return allowEmpty;
        }

        String trimmedContact = contactInfo.trim();

        // Check for common contact info patterns
        // Phone number patterns (various formats)
        String phonePattern = "^[\\+]?[\\d\\s\\-\\(\\)\\.]{7,20}$";
        
        // Email pattern (basic)
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        
        // Mixed contact info (phone and email separated by comma or semicolon)
        String mixedPattern = "^[\\w\\s\\+\\-\\(\\)\\.@,;]+$";

        // Check if it matches any valid pattern
        if (trimmedContact.matches(phonePattern) || 
            trimmedContact.matches(emailPattern) ||
            trimmedContact.matches(mixedPattern)) {
            
            // Additional validation for mixed format
            if (trimmedContact.contains("@") && trimmedContact.contains(",")) {
                // Split by comma and validate each part
                String[] parts = trimmedContact.split("[,;]");
                for (String part : parts) {
                    String trimmedPart = part.trim();
                    if (!trimmedPart.matches(phonePattern) && !trimmedPart.matches(emailPattern)) {
                        return false;
                    }
                }
            }
            
            return true;
        }

        return false;
    }
}