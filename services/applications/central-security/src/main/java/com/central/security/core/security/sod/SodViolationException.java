package com.central.security.core.security.sod;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a role-assignment would violate a Separation-of-Duties constraint.
 * Results in HTTP 422 Unprocessable Entity so clients can distinguish SoD failures
 * from generic validation (400) or authorization (403) errors.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SodViolationException extends RuntimeException {

    public SodViolationException(final String message) {
        super(message);
    }
}

