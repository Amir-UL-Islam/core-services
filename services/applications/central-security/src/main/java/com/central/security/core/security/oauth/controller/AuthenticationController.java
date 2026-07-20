package com.central.security.core.security.oauth.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.repository.RoleRepository;
import com.central.security.core.security.mfa.LoginMfaService;
import com.central.security.core.security.mfa.otp.dto.OtpChallengeResponse;
import com.central.security.core.security.jwt.CustomUserDetailsService;
import com.central.security.core.security.jwt.JwtTokenService;
import com.central.security.core.security.jwt.RefreshTokenRequest;
import com.central.security.core.security.oauth.clientcredentials.service.ClientCredentialsService;
import com.central.security.core.security.oauth.UserRoles;
import com.central.security.core.security.oauth.dto.AuthenticationRequest;
import com.central.security.core.security.oauth.dto.AuthenticationResponse;
import com.central.security.core.security.oauth.dto.AuthenticationSocialRequest;
import com.central.security.core.security.oauth.service.RegistrationService;
import com.central.security.core.security.sod.SodConstraintService;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;


@RestController
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication and token management")
@RequestMapping("/api/v1")
public class AuthenticationController {

    private final AuthenticationProvider authenticationProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenService jwtTokenService;
    private final Environment environment;
    private final UsersRepository usersRepository;
    private final String baseHost;
    private final RoleRepository roleRepository;
    private final LoginMfaService loginMfaService;
    private final SodConstraintService sodConstraintService;
    private final RegistrationService registrationService;
    private final ClientCredentialsService clientCredentialsService;
    private final RestClient googleClient = RestClient.builder()
            .baseUrl("https://oauth2.googleapis.com/")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public AuthenticationController(
            final AuthenticationProvider authenticationProvider,
            final CustomUserDetailsService customUserDetailsService,
            final JwtTokenService jwtTokenService,
            final Environment environment,
            final UsersRepository usersRepository,
            @Value("${app.baseHost}") final String baseHost,
            final RoleRepository roleRepository,
            final LoginMfaService loginMfaService,
            final SodConstraintService sodConstraintService,
            final RegistrationService registrationService,
            final ClientCredentialsService clientCredentialsService
    ) {
        this.authenticationProvider = authenticationProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenService = jwtTokenService;
        this.environment = environment;
        this.usersRepository = usersRepository;
        this.baseHost = baseHost;
        this.roleRepository = roleRepository;
        this.loginMfaService = loginMfaService;
        this.sodConstraintService = sodConstraintService;
        this.registrationService = registrationService;
        this.clientCredentialsService = clientCredentialsService;
    }

    @PostMapping("/authenticate")
    public AuthenticationResponse authenticate(
            @RequestBody @Valid final AuthenticationRequest authenticationRequest) {
        try {
            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getUsername(), authenticationRequest.getPassword()));
        } catch (final BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        } catch (final DisabledException ex) {
            // Account exists, and the password is correct but has not completed verification.
            // Re-issue an OTP — the client submits it to POST /register/verify to activate.
            log.info("login attempt for unverified account: {}", authenticationRequest.getUsername());
            return buildVerificationChallengeResponse(authenticationRequest.getUsername());
        }

        final Users userDetails = customUserDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final var challenge = loginMfaService.enforceLoginMfa(
                userDetails,
                authenticationRequest.getOtp(),
                authenticationRequest.getChallengeId(),
                authenticationRequest.getOtpChannel()
        );
        if (challenge.isPresent()) {
            return buildMfaChallengeResponse(challenge.get());
        }
        final Set<String> activeRoles = resolveAndValidateActiveRoles(
                userDetails, authenticationRequest.getActiveRoles());
        return buildAuthenticationResponse(userDetails, "direct", null, null, activeRoles);
    }

    @Transactional
    @PostMapping(value = "/oauth/token", params = "grant_type=password",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> passwordGrant(
            @RequestParam("username") final String username,
            @RequestParam("password") final String password,
            @RequestParam(value = "scope", required = false) final String scope,
            @RequestParam(value = "otp", required = false) final String otp,
            @RequestParam(value = "otp_channel", required = false) final String otpChannel,
            @RequestParam(value = "challenge_id", required = false) final String challengeId) {
        try {
            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (final BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        final Users userDetails = customUserDetailsService.loadUserByUsername(username);
        final var challenge = loginMfaService.enforceLoginMfa(userDetails, otp, challengeId, otpChannel);
        if (challenge.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "MFA challenge required. Use /authenticate JSON flow for challenge lifecycle.");
        }
        final String accessToken = jwtTokenService.generateAccessToken(userDetails, "direct", null);
        final String refreshToken = jwtTokenService.generateRefreshToken(userDetails, "direct", null);

        final String resolvedScope = scope != null ? scope
                : String.join(" ", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());

        return Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "bearer",
                "expires_in", jwtTokenService.accessTokenValiditySeconds(),
                "scope", resolvedScope
        );
    }

    @PostMapping(value = "/oauth/token", params = "grant_type=client_credentials",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> clientCredentialsGrant(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authorization,
            @RequestParam(value = "client_id", required = false) final String clientIdParam,
            @RequestParam(value = "client_secret", required = false) final String clientSecretParam,
            @RequestParam(value = "scope", required = false) final String scope
    ) {
        final String[] credentials = extractClientCredentials(authorization, clientIdParam, clientSecretParam);
        final String clientId = credentials[0];
        final String clientSecret = credentials[1];

        try {
            final ClientCredentialsService.TokenIssueResult token =
                    clientCredentialsService.issueToken(clientId, clientSecret, scope);
            return Map.of(
                    "access_token", token.accessToken(),
                    "token_type", "bearer",
                    "expires_in", token.expiresInSeconds(),
                    "scope", String.join(" ", token.scopes())
            );
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public AuthenticationResponse refreshToken(@RequestBody @Valid final RefreshTokenRequest refreshTokenRequest) {
        final DecodedJWT refreshTokenJwt = jwtTokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken());
        if (refreshTokenJwt == null || refreshTokenJwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        final String loginType = refreshTokenJwt.getClaim("login_type").asString();
        if (loginType == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        final Users userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(refreshTokenJwt.getSubject());
        } catch (final UsernameNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (!jwtTokenService.isRefreshTokenVersionValid(refreshTokenJwt, userDetails.getRefreshTokenVersion())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }

        return buildAuthenticationResponse(userDetails, loginType, null, null, null);
    }

    @Transactional
    @PostMapping("/authenticateGoogle")
    public AuthenticationResponse authenticateGoogle(
            @RequestBody @Valid final AuthenticationSocialRequest authenticationSocialRequest) {
        log.info("exchanging google code");
        final String providerId = "google";
        final String clientId = environment.getProperty("app." + providerId + ".client-id");
        final String clientSecret = environment.getProperty("app." + providerId + ".client-secret");
        final RestClient.ResponseSpec accessTokenSpec = googleClient.post()
                .uri("token")
                .body(Map.of("client_id", clientId, "client_secret", clientSecret,
                        "redirect_uri", baseHost + "/completeLogin?provider=google",
                        "grant_type", "authorization_code",
                        "code", authenticationSocialRequest.getCode()))
                .retrieve();
        final Map<String, Object> accessTokenResponse = accessTokenSpec.body(new ParameterizedTypeReference<>() {
        });

        log.info("validating google access token");
        final RestClient.ResponseSpec tokenInfoSpec = googleClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("tokeninfo")
                        .queryParam("id_token", accessTokenResponse.get("id_token"))
                        .build())
                .retrieve();
        final Map<String, Object> tokenInfoResponse = tokenInfoSpec.body(new ParameterizedTypeReference<>() {
        });
        if (!clientId.equals(tokenInfoResponse.get("aud"))) {
            log.warn("google app id not matching");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        final String subject = tokenInfoResponse.get("sub").toString();
        final Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(tokenInfoResponse.get("exp").toString()));
        if (expiresAt.isBefore(Instant.now())) {
            log.warn("google token has expired");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return synchronizeUserAndGetToken(providerId, subject, tokenInfoResponse, expiresAt);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private AuthenticationResponse buildAuthenticationResponse(
            final Users userDetails,
            final String loginType,
            final Duration accessTokenValidity,
            final Duration refreshTokenValidity,
            final Set<String> activeRoles
    ) {
        final AuthenticationResponse response = new AuthenticationResponse();
        final String accessToken = (activeRoles != null && !activeRoles.isEmpty())
                ? jwtTokenService.generateAccessTokenForActiveRoles(userDetails, loginType, accessTokenValidity, activeRoles)
                : jwtTokenService.generateAccessToken(userDetails, loginType, accessTokenValidity);
        response.setAccessToken(accessToken);
        response.setUserId(userDetails.getId());
        response.setRefreshToken(jwtTokenService.generateRefreshToken(userDetails, loginType, refreshTokenValidity));
        response.setAvailableRoles(userDetails.getRole().stream()
                .map(Role::getName).sorted().toList());
        return response;
    }

    /**
     * Builds a response instructing the client to complete OTP verification.
     * A fresh OTP is dispatched to the user's registered contact (email or SMS).
     */
    private AuthenticationResponse buildVerificationChallengeResponse(final String username) {
        final OtpChallengeResponse challenge = registrationService.reissueVerificationChallenge(username);
        final AuthenticationResponse response = new AuthenticationResponse();
        response.setRequiresVerification(true);
        response.setVerificationChallengeId(challenge.getChallengeId());
        response.setVerificationChannel(challenge.getFactor().name());
        response.setVerificationMessage(
                "Your account is not yet verified. A verification code has been sent via "
                + challenge.getFactor().name().toLowerCase() + ".");
        response.setVerificationExpiresInSeconds(challenge.getExpiresInSeconds());
        return response;
    }

    private AuthenticationResponse buildMfaChallengeResponse(final OtpChallengeResponse challenge) {
        final AuthenticationResponse response = new AuthenticationResponse();
        response.setRequiresMfa(true);
        response.setMfaChallengeId(challenge.getChallengeId());
        response.setMfaChannel(challenge.getFactor().name());
        response.setMfaMessage(challenge.getMessage());
        response.setMfaExpiresInSeconds(challenge.getExpiresInSeconds());
        return response;
    }

    private Set<String> resolveAndValidateActiveRoles(
            final Users user, final List<String> requestedActiveRoles) {
        if (requestedActiveRoles == null || requestedActiveRoles.isEmpty()) {
            return null;
        }
        final Set<String> userRoleNames = new java.util.HashSet<>();
        user.getRole().forEach(r -> userRoleNames.add(r.getName().toUpperCase()));
        for (final String requested : requestedActiveRoles) {
            if (!userRoleNames.contains(requested.toUpperCase())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User does not hold role: " + requested);
            }
        }
        sodConstraintService.assertNoDynamicConflict(requestedActiveRoles);
        return new java.util.HashSet<>(requestedActiveRoles);
    }

    protected AuthenticationResponse synchronizeUserAndGetToken(final String loginType,
                                                                final String subject, final Map<String, Object> tokeninfoResponse, final Instant expiresAt) {
        Users users = usersRepository.findByEmail(subject);
        if (users == null) {
            log.info("adding new user after successful authentication: {}", subject);
            final var userRole = roleRepository.findByName(UserRoles.USER);
            if (userRole.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "USER role missing");
            }
            users = new Users();
            users.setEmail(subject);
            users.setPhone("");
            users.setGender(tokeninfoResponse.get("gender").toString());
            users.setUsername(subject);
            users.setName(tokeninfoResponse.get("name").toString());
            users.setEmailVerified(true);
            users.setPhoneVerified(false);
            users.setAccountEnabled(true);
            users.setSmsMfaEnabled(false);
            users.setEmailMfaEnabled(false);
            users.setRole(new LinkedHashSet<>(Set.of(userRole.get())));
            users.setTokenVersion(1);
        } else {
            log.info("updating existing user after successful authentication: {}", subject);
        }
        usersRepository.save(users);

        final Users userDetails = customUserDetailsService.loadUserByUsername(subject);
        final Duration validity = Duration.between(Instant.now(), expiresAt);
        return buildAuthenticationResponse(userDetails, loginType, validity, null, null);
    }

    private String[] extractClientCredentials(final String authorization,
                                              final String clientIdParam,
                                              final String clientSecretParam) {
        if (StringUtils.hasText(authorization) && authorization.startsWith("Basic ")) {
            try {
                final String base64Credentials = authorization.substring(6).trim();
                final String decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                final int separator = decoded.indexOf(':');
                if (separator > 0) {
                    final String id = decoded.substring(0, separator);
                    final String secret = decoded.substring(separator + 1);
                    if (StringUtils.hasText(id) && StringUtils.hasText(secret)) {
                        return new String[]{id, secret};
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Basic Authorization header");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Basic Authorization header");
        }

        if (StringUtils.hasText(clientIdParam) && StringUtils.hasText(clientSecretParam)) {
            return new String[]{clientIdParam, clientSecretParam};
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "client_id and client_secret are required");
    }
}
