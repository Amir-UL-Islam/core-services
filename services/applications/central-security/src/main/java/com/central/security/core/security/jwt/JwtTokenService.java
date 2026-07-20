package com.central.security.core.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.users.model.entity.Users;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class JwtTokenService {

    // Token validity durations can be configured as needed; these are defaults.
    private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(600);
    private static final Duration MAX_ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(600);
    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofHours(300);
    private static final Duration MAX_REFRESH_TOKEN_VALIDITY = Duration.ofHours(300);

    private final Algorithm rsa256;
    private final JWTVerifier verifier;

    public JwtTokenService(
            @Value("classpath:certs/public.pem") final RSAPublicKey publicKey,
            @Value("classpath:certs/private.pem") final RSAPrivateKey privateKey
    ) {
        this.rsa256 = Algorithm.RSA256(publicKey, privateKey);
        this.verifier = JWT.require(this.rsa256).build();
    }

    public String generateAccessToken(
            final Users userDetails,
            final String loginType,
            final Duration validity
    ) {
        return generateToken(userDetails, loginType, TokenType.ACCESS, validity,
                ACCESS_TOKEN_VALIDITY, MAX_ACCESS_TOKEN_VALIDITY, null);
    }

    /**
     * Generate an access token scoped to a specific subset of the user's roles (Dynamic SoD).
     * Only authorities from the {@code activeRoles} set are included in the JWT.
     * The caller MUST have already validated:
     *  1. The user actually holds all {@code activeRoles}.
     *  2. No dynamic SoD constraint is violated by the selected role combination.
     */
    public String generateAccessTokenForActiveRoles(
            final Users userDetails,
            final String loginType,
            final Duration validity,
            final Set<String> activeRoles
    ) {
        return generateToken(userDetails, loginType, TokenType.ACCESS, validity,
                ACCESS_TOKEN_VALIDITY, MAX_ACCESS_TOKEN_VALIDITY, activeRoles);
    }

    public String generateRefreshToken(
            final Users userDetails,
            final String loginType,
            final Duration validity
    ) {
        return generateToken(userDetails, loginType, TokenType.REFRESH, validity,
                REFRESH_TOKEN_VALIDITY, MAX_REFRESH_TOKEN_VALIDITY, null);
    }

    public String generateClientCredentialsAccessToken(
            final String clientId,
            final Set<String> authorities,
            final Set<String> scopes,
            final Duration validity
    ) {
        final Instant now = Instant.now();
        final Duration tokenValidity = validity == null ? ACCESS_TOKEN_VALIDITY
                : (validity.compareTo(MAX_ACCESS_TOKEN_VALIDITY) > 0 ? MAX_ACCESS_TOKEN_VALIDITY : validity);

        final List<String> authorityList = new ArrayList<>();
        if (authorities != null) {
            authorityList.addAll(authorities.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
        }

        final List<String> scopeList = new ArrayList<>();
        if (scopes != null) {
            scopeList.addAll(scopes.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
        }

        return JWT.create()
                .withSubject(clientId)
                .withClaim("client_id", clientId)
                .withClaim("grant_type", "client_credentials")
                .withClaim("token_type", TokenType.ACCESS.getValue())
                .withArrayClaim("authorities", authorityList.toArray(String[]::new))
                .withArrayClaim("scope", scopeList.toArray(String[]::new))
                .withClaim("login_type", "client_credentials")
                .withIssuer("app")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(tokenValidity))
                .sign(this.rsa256);
    }

    private String generateToken(
            final Users userDetails,
            final String loginType,
            final TokenType tokenType,
            final Duration validity,
            final Duration defaultValidity,
            final Duration maxValidity,
            final Set<String> activeRoles  // null = all roles; non-null = restrict to these role names
    ) {
        final Instant now = Instant.now();
        final Duration tokenValidity = validity == null ? defaultValidity
                : (validity.compareTo(maxValidity) > 0 ? maxValidity : validity);
        // Access tokens carry tokenVersion; refresh tokens carry refreshTokenVersion so each
        // can be independently revoked without affecting the other.
        final int versionClaim = tokenType == TokenType.REFRESH
                ? userDetails.getRefreshTokenVersion()
                : userDetails.getTokenVersion();
        // Determine effective roles and authorities — scoped by activeRoles if provided
        final List<String> effectiveRoleNames = userDetails.getRole().stream()
                .map(Role::getName)
                .filter(name -> activeRoles == null || activeRoles.stream()
                        .anyMatch(ar -> ar.equalsIgnoreCase(name)))
                .distinct()
                .toList();

        final Set<String> activeRoleSet = activeRoles != null
                ? Set.copyOf(activeRoles.stream().map(String::toUpperCase).toList())
                : null;

        final String[] authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> !authority.startsWith("ROLE_"))
                // When activeRoles is set, only include authorities from those roles.
                // We do this by matching via the role-projection ROLE_ authority, which was
                // already filtered out — we rely on the fact that getAuthorities() includes
                // all privileges of all roles. For scoped tokens we re-derive from the user's
                // active roles directly.
                .distinct()
                .toArray(String[]::new);

        // For scoped tokens: re-derive authorities from active roles only
        final String[] scopedAuthorities;
        if (activeRoles != null && !activeRoles.isEmpty()) {
            scopedAuthorities = userDetails.getRole().stream()
                    .filter(r -> activeRoles.stream().anyMatch(ar -> ar.equalsIgnoreCase(r.getName())))
                    .flatMap(r -> r.getPrivilege().stream())
                    .map(Privilege::getName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toArray(String[]::new);
        } else {
            scopedAuthorities = authorities;
        }

        return JWT.create()
                .withSubject(userDetails.getUsername())
                .withClaim("login_type", loginType)
                .withClaim("token_type", tokenType.getValue())
                .withClaim("token_version", versionClaim)
                .withArrayClaim("authorities", scopedAuthorities)
                .withArrayClaim("roles", effectiveRoleNames.toArray(String[]::new))
                .withClaim("active_roles_scoped", activeRoles != null)
                .withIssuer("app")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(tokenValidity))
                .sign(this.rsa256);
    }

    public DecodedJWT validateAccessToken(final String token) {
        return validateToken(token, TokenType.ACCESS);
    }

    public DecodedJWT validateRefreshToken(final String token) {
        return validateToken(token, TokenType.REFRESH);
    }

    public long accessTokenValiditySeconds() {
        return ACCESS_TOKEN_VALIDITY.getSeconds();
    }

    private DecodedJWT validateToken(final String token, final TokenType expectedType) {
        try {
            final DecodedJWT jwt = verifier.verify(token);
            final String tokenType = jwt.getClaim("token_type").asString();
            if (!expectedType.getValue().equals(tokenType)) {
                log.warn("Invalid token type: Expected {}, Found: {}", expectedType.getValue(),
                        tokenType);
                return null;
            }
            return jwt;
        } catch (final JWTVerificationException verificationEx) {
            log.warn("token invalid: {}", verificationEx.getMessage());
            return null;
        }
    }

    /**
     * Validate token version claim against current user token version.
     * This ensures tokens are invalidated when a user's permissions change.
     *
     * @param decodedToken The decoded JWT token
     * @param currentUserTokenVersion The current token version from database
     * @return true if token version matches (token is fresh), false if stale
     */
    public boolean isTokenVersionValid(final DecodedJWT decodedToken, final int currentUserTokenVersion) {
        try {
            final int tokenVersion = decodedToken.getClaim("token_version").asInt();
            if (tokenVersion != currentUserTokenVersion) {
                log.warn("Token version mismatch: token has version {}, user has version {}",
                        tokenVersion, currentUserTokenVersion);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to extract token version from JWT", e);
            return false;
        }
    }

    /**
     * Validate refresh token version for refresh token endpoint.
     * Separate from access token version to allow independent revocation strategies.
     *
     * @param decodedToken The decoded JWT token
     * @param currentUserRefreshTokenVersion The current refresh token version from database
     * @return true if refresh token version matches, false if stale
     */
    public boolean isRefreshTokenVersionValid(final DecodedJWT decodedToken, final int currentUserRefreshTokenVersion) {
        try {
            final int tokenVersion = decodedToken.getClaim("token_version").asInt();
            if (tokenVersion != currentUserRefreshTokenVersion) {
                log.warn("Refresh token version mismatch: token has version {}, user has version {}",
                        tokenVersion, currentUserRefreshTokenVersion);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to extract token version from refresh JWT", e);
            return false;
        }
    }

    @Getter
    private enum TokenType {
        ACCESS("access"),
        REFRESH("refresh");

        private final String value;

        TokenType(final String value) {
            this.value = value;
        }

    }

}
