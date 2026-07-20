package com.central.security.core.security.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Validates optional gender input against supported values.
 */
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = GenderValue.GenderValueValidator.class)
public @interface GenderValue {

    String message() default "{registration.gender.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GenderValueValidator implements ConstraintValidator<GenderValue, String> {

        private static final Set<String> ALLOWED = Set.of("MALE", "FEMALE", "OTHER");

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }
            return ALLOWED.contains(value.trim().toUpperCase());
        }
    }
}

