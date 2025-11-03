package com.company.employeeportal.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * Validator for StrongPassword annotation.
 * Validates password strength requirements.
 */
@Component
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigits;
    private boolean requireSpecialChars;

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigits = constraintAnnotation.requireDigits();
        this.requireSpecialChars = constraintAnnotation.requireSpecialChars();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            return true; // Let @NotBlank handle null/empty validation
        }

        // Check minimum length
        if (password.length() < minLength) {
            addConstraintViolation(context, "Password must be at least " + minLength + " characters long");
            return false;
        }

        // Check uppercase requirement
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            addConstraintViolation(context, "Password must contain at least one uppercase letter");
            return false;
        }

        // Check lowercase requirement
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            addConstraintViolation(context, "Password must contain at least one lowercase letter");
            return false;
        }

        // Check digit requirement
        if (requireDigits && !password.matches(".*\\d.*")) {
            addConstraintViolation(context, "Password must contain at least one digit");
            return false;
        }

        // Check special character requirement
        if (requireSpecialChars && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            addConstraintViolation(context, "Password must contain at least one special character");
            return false;
        }

        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
