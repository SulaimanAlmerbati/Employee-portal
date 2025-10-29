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

        // New pattern for country code + numbers only (e.g., +15551234567)
        String phoneWithCountryCodePattern = "^\\+\\d{1,4}\\d{7,15}$";
        
        // Legacy phone number patterns (for backward compatibility)
        String legacyPhonePattern = "^[\\+]?[\\d\\s\\-\\(\\)\\.]{7,20}$";
        
        // Email pattern (basic)
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

        // Check if it matches the new preferred format first
        if (trimmedContact.matches(phoneWithCountryCodePattern)) {
            return true;
        }
        
        // Check legacy formats for backward compatibility
        if (trimmedContact.matches(legacyPhonePattern) || 
            trimmedContact.matches(emailPattern)) {
            return true;
        }

        // Set custom error message for invalid format
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
            "Contact info must be a valid phone number with country code (e.g., +15551234567) or email address")
            .addConstraintViolation();

        return false;
    }
}