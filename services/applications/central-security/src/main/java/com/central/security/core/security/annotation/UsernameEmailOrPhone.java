package com.central.security.core.security.annotation;

import com.central.security.core.security.validation.SecurityValidationPatterns;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/**
 * Validates that the input is a valid username, email address, or phone number.
 * This is used for forgot password flows where users can identify themselves
 * using any of these three identifiers.
 */
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UsernameEmailOrPhone.UsernameEmailOrPhoneValidator.class)
public @interface UsernameEmailOrPhone {

    String message() default "Must be a valid username, email address, or phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UsernameEmailOrPhoneValidator implements ConstraintValidator<UsernameEmailOrPhone, String> {

        private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return false;
            }

            String trimmed = value.trim();

            if (trimmed.length() > 255) {
                return false;
            }

            if (isValidEmail(trimmed)) {
                return true;
            }

            if (isValidPhone(trimmed)) {
                return true;
            }

            return isValidUsername(trimmed);
        }

        private boolean isValidEmail(String value) {
            return value.matches(EMAIL_REGEX);
        }

        private boolean isValidPhone(String value) {
            return value.matches(SecurityValidationPatterns.BD_IN_CONTACT_REGEX);
        }

        private boolean isValidUsername(String value) {
            if (value.length() < 6 || value.length() > 255) {
                return false;
            }

            if (!Character.isLetterOrDigit(value.charAt(0))) {
                return false;
            }

            return value.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*$");
        }
    }
}
