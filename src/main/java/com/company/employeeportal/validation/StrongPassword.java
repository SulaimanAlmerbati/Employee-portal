package com.company.employeeportal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for strong password requirements.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    
    String message() default "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum length requirement.
     */
    int minLength() default 8;
    
    /**
     * Whether to require uppercase letters.
     */
    boolean requireUppercase() default true;
    
    /**
     * Whether to require lowercase letters.
     */
    boolean requireLowercase() default true;
    
    /**
     * Whether to require digits.
     */
    boolean requireDigits() default true;
    
    /**
     * Whether to require special characters.
     */
    boolean requireSpecialChars() default true;
}