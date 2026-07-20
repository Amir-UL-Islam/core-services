package com.central.security.core.security.oauth.clientcredentials.service;

import com.central.security.core.security.jwt.JwtTokenService;
import com.central.security.core.security.oauth.clientcredentials.config.ClientCredentialsProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientCredentialsService {

    public static final String CLIENT_CREDENTIALS_AUTHORITY = "CLIENT_CREDENTIALS";

    private final ClientCredentialsProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public TokenIssueResult issueToken(final String clientId,
                                       final String clientSecret,
                                       final String requestedScope) {
        final ClientCredentialsProperties.ClientDefinition client = loadClient(clientId);
        if (!isSecretValid(clientSecret, client.getSecret(), client.isSecretEncoded())) {
            throw new BadCredentialsException("Invalid client credentials");
        }

        final Set<String> grantedScopes = resolveScopes(client.getScopes(), requestedScope);
        final Set<String> authorities = new LinkedHashSet<>();
        authorities.add(CLIENT_CREDENTIALS_AUTHORITY);
        client.getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(authorities::add);
        grantedScopes.stream()
                .map(scope -> "SCOPE_" + scope)
                .forEach(authorities::add);

        final long expiresIn = properties.getAccessTokenTtlSeconds();
        final String token = jwtTokenService.generateClientCredentialsAccessToken(
                clientId,
                authorities,
                grantedScopes,
                Duration.ofSeconds(expiresIn)
        );

        return new TokenIssueResult(token, expiresIn, grantedScopes, authorities);
    }

    private ClientCredentialsProperties.ClientDefinition loadClient(final String clientId) {
        final ClientCredentialsProperties.ClientDefinition client = properties.getClients().get(clientId);
        if (client == null || !client.isEnabled()) {
            throw new BadCredentialsException("Invalid client credentials");
        }
        return client;
    }

    private boolean isSecretValid(final String rawSecret, final String configuredSecret, final boolean encoded) {
        if (rawSecret == null || configuredSecret == null) {
            return false;
        }
        if (encoded) {
            return passwordEncoder.matches(rawSecret, configuredSecret);
        }
        return MessageDigest.isEqual(
                rawSecret.getBytes(StandardCharsets.UTF_8),
                configuredSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Set<String> resolveScopes(final Set<String> allowedScopes, final String requestedScope) {
        if (allowedScopes == null || allowedScopes.isEmpty()) {
            return Set.of();
        }
        if (requestedScope == null || requestedScope.isBlank()) {
            return new LinkedHashSet<>(allowedScopes);
        }

        final Set<String> requestedScopes = Arrays.stream(requestedScope.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        if (!allowedScopes.containsAll(requestedScopes)) {
            throw new BadCredentialsException("Requested scope exceeds allowed scopes");
        }
        return requestedScopes;
    }

    public record TokenIssueResult(String accessToken,
                                   long expiresInSeconds,
                                   Set<String> scopes,
                                   Set<String> authorities) {
    }
}

