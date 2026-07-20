package com.central.security.core.security.oauth.clientcredentials.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configurable machine-to-machine OAuth2 clients for client-credentials grant.
 */
@Component
@ConfigurationProperties(prefix = "security.oauth2.client-credentials")
@Validated
@Getter
@Setter
public class ClientCredentialsProperties {

    @Positive
    private long accessTokenTtlSeconds = 1800;

    private Map<String, ClientDefinition> clients = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class ClientDefinition {

        @NotBlank
        private String secret;

        /**
         * If true, secret uses Spring PasswordEncoder format (e.g. {bcrypt}...).
         */
        private boolean secretEncoded = false;

        private boolean enabled = true;

        /**
         * OAuth2 scopes included in the JWT as SCOPE_* authorities.
         */
        private Set<String> scopes = new LinkedHashSet<>();

        /**
         * API privilege names used by ACL URL policy checks.
         */
        private Set<String> authorities = new LinkedHashSet<>();
    }
}

