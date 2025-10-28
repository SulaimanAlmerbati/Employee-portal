package com.company.employeeportal.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Validator for PasswordMatches annotation.
 * Validates that password and confirm password fields match.
 */
@Component
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.passwordField = constraintAnnotation.password();
        this.confirmPasswordField = constraintAnnotation.confirmPassword();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        try {
            String password = getFieldValue(obj, passwordField);
            String confirmPassword = getFieldValue(obj, confirmPasswordField);

            if (password == null && confirmPassword == null) {
                return true;
            }

            if (password == null || confirmPassword == null) {
                return false;
            }

            return password.equals(confirmPassword);

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
}