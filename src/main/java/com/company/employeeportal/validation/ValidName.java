package com.company.employeeportal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for validating name format.
 * Ensures name contains only letters, spaces, hyphens, and apostrophes.
 */
@Documented
@Constraint(validatedBy = ValidNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {
    
    String message() default "Name must contain only letters, spaces, hyphens, and apostrophes";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow empty/null values.
     */
    boolean allowEmpty() default true;
}