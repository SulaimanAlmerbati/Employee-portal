package com.company.employeeportal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to check if email is unique for user updates.
 * This annotation is used at the class level and requires both email and userId fields.
 */
@Documented
@Constraint(validatedBy = UniqueEmailForUpdateValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmailForUpdate {
    
    String message() default "Email already exists";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Field name for email.
     */
    String emailField() default "email";
    
    /**
     * Field name for user ID.
     */
    String userIdField() default "userId";
}
