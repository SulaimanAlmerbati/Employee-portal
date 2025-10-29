package com.company.employeeportal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for validating contact information format.
 * Supports phone numbers, email addresses, and other contact formats.
 */
@Documented
@Constraint(validatedBy = ValidContactInfoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContactInfo {
    
    String message() default "Contact information format is invalid";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow empty/null values.
     */
    boolean allowEmpty() default true;
}
