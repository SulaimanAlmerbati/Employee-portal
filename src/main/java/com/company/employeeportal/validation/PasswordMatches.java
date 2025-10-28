package com.company.employeeportal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to check if password and confirm password match.
 */
@Documented
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {
    
    String message() default "Password and confirm password do not match";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Field name for password.
     */
    String password() default "newPassword";
    
    /**
     * Field name for confirm password.
     */
    String confirmPassword() default "confirmPassword";
}