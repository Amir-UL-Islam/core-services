package com.central.security.core.security.oauth.clientcredentials.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as machine-client enabled.
 *
 * <p>User-based access keeps working as usual. For JWTs issued via client_credentials,
 * this annotation enforces the configured scope constraints.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MakeThisAvailableForMachineClient {

    /**
     * Required OAuth2 scopes for machine tokens (without SCOPE_ prefix).
     */
    String[] scopes() default {};
}

