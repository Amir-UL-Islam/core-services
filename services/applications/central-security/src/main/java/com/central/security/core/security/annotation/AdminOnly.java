package com.central.security.core.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as requiring admin-level privileges (defense-in-depth layer).
 *
 * Accepted roles: ADMIN and SUPER_ADMIN (RBAC3 role hierarchy ensures SUPER_ADMIN
 * inherits all ADMIN capabilities).
 *
 * Both {@code hasRole('X')} and {@code hasAnyAuthority('ROLE_X')} evaluate the
 * same Spring-prefixed authority, so we use the more readable {@code hasAnyRole}
 * form exclusively.
 *
 * Requires {@code @EnableMethodSecurity(prePostEnabled = true)} on the config class.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public @interface AdminOnly {
    /**
     * Optional human-readable reason for the access requirement (for documentation).
     */
    String reason() default "Administrative operation";
}

