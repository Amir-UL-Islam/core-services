package com.central.security.core.security.oauth.service;

import com.central.security.core.role.model.entity.Role;
import com.central.security.core.security.jwt.CustomUserDetailsService;
import com.central.security.core.security.jwt.JwtTokenService;
import com.central.security.core.security.mfa.MfaFactorType;
import com.central.security.core.security.mfa.MfaPolicyResolver;
import com.central.security.core.security.mfa.otp.OtpChallenge;
import com.central.security.core.security.mfa.otp.OtpChallengeRepository;
import com.central.security.core.security.mfa.otp.OtpChallengeService;
import com.central.security.core.security.mfa.otp.OtpPurpose;
import com.central.security.core.security.mfa.otp.dto.OtpChallengeResponse;
import com.central.security.core.security.oauth.dto.AuthenticationResponse;
import com.central.security.core.security.oauth.dto.RegistrationInitRequest;
import com.central.security.core.security.oauth.dto.RegistrationInitResponse;
import com.central.security.core.security.oauth.dto.RegistrationVerifyRequest;
import com.central.security.core.security.policy.config.SecurityPolicyKeys;
import com.central.security.core.security.policy.config.SecurityPolicyService;
import com.central.security.core.role.repository.RoleRepository;
import com.central.security.core.security.oauth.UserRoles;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;


@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final MfaPolicyResolver mfaPolicyResolver;
    private final OtpChallengeService otpChallengeService;
    private final OtpChallengeRepository otpChallengeRepository;
    private final SecurityPolicyService securityPolicyService;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    // -----------------------------------------------------------------------
    // Init — Step 1 of registration
    // -----------------------------------------------------------------------

    @Transactional
    public RegistrationInitResponse initRegistration(final RegistrationInitRequest request) {
        final boolean verificationRequired = securityPolicyService.getBooleanOrDefault(
                SecurityPolicyKeys.REGISTRATION_VERIFICATION_REQUIRED, false);

        log.info("Starting registration for user: {} (verificationRequired={})",
                request.getUsername(), verificationRequired);

        final Users users = buildOrReuseUserRecord(request);

        if (!verificationRequired) {
            return completeInstantly(users, request);
        }
        return issueOtpChallenge(users, request);
    }

    // -----------------------------------------------------------------------
    // Verify — Step 2 of registration (only when verificationRequired=true)
    // -----------------------------------------------------------------------

    /**
     * Validates the OTP, enables the account, and returns tokens so the client
     * does not need a separate {@code /authenticate} round-trip.
     */
    @Transactional
    public AuthenticationResponse verifyRegistration(final RegistrationVerifyRequest request) {
        final OtpChallenge challenge = otpChallengeRepository.findByChallengeId(request.getChallengeId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Registration challenge not found"));

        otpChallengeService.verifyChallenge(
                request.getChallengeId(),
                challenge.getUsername(),
                OtpPurpose.REGISTRATION,
                request.getOtp()
        );

        final Users users = usersRepository.findByUsernameIgnoreCase(challenge.getUsername());
        if (users == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "User not found for challenge");
        }

        users.setAccountEnabled(true);
        if (challenge.getFactor() == MfaFactorType.EMAIL) {
            users.setEmailVerified(true);
        }
        if (challenge.getFactor() == MfaFactorType.SMS) {
            users.setPhoneVerified(true);
        }
        usersRepository.save(users);

        return buildTokenResponse(users);
    }

    // -----------------------------------------------------------------------
    // Re-verification — for accounts that are disabled (accountEnabled=false)
    // Called by AuthenticationController when a DisabledException is caught.
    // -----------------------------------------------------------------------

    /**
     * Re-issues a registration OTP for a user whose account is not yet enabled.
     * Always requires OTP regardless of the {@code registration.verification.required}
     * policy — a disabled account must prove ownership of a contact channel.
     */
    @Transactional
    public OtpChallengeResponse reissueVerificationChallenge(final String username) {
        final Users users = usersRepository.findByUsernameIgnoreCase(username);
        if (users == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "User not found");
        }
        final MfaFactorType channel = resolveReissueChannel(users);
        final String destination = resolveDestinationForUser(users, channel);

        log.info("re-issuing verification OTP for disabled user: {} via {}", username, channel);
        return otpChallengeService.issueChallenge(users, channel, OtpPurpose.REGISTRATION, destination);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Users buildOrReuseUserRecord(final RegistrationInitRequest request) {
        Users users = usersRepository.findByUsernameIgnoreCase(request.getUsername());
        if (users != null && Boolean.TRUE.equals(users.getAccountEnabled())) {
            throw new ResponseStatusException(CONFLICT, "Username already registered");
        }
        if (users == null) {
            users = new Users();
            users.setUsername(request.getUsername());
        }

        users.setName(request.getName());
        users.setEmail(request.getEmail());
        users.setPhone(request.getPhone());
        users.setGender(request.getGender());
        users.setRelation(request.getRelation());
        users.setPassword(passwordEncoder.encode(request.getPassword()));
        users.setAccountEnabled(false);
        users.setEmailVerified(false);
        users.setPhoneVerified(false);
        users.setTwoFactorEnabled(false);
        users.setSmsMfaEnabled(false);
        users.setEmailMfaEnabled(false);

        final var userRole = roleRepository.findByName(UserRoles.USER);
        if (userRole.isEmpty()) {
            throw new IllegalStateException("USER role missing. Check RoleLoader initialization order.");
        }
        users.setRole(new LinkedHashSet<>(Set.of(userRole.get())));
        users.setTokenVersion(1);
        return usersRepository.save(users);
    }

    RegistrationInitResponse completeInstantly(final Users users,
                                               final RegistrationInitRequest request) {
        users.setAccountEnabled(true);
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            users.setEmailVerified(true);
        }
        usersRepository.save(users);

        final Users loaded = customUserDetailsService.loadUserByUsername(users.getUsername());

        final RegistrationInitResponse response = new RegistrationInitResponse();
        response.setRequiresVerification(false);
        response.setUserId(loaded.getId());
        response.setAccessToken(jwtTokenService.generateAccessToken(loaded, "registration", null));
        response.setRefreshToken(jwtTokenService.generateRefreshToken(loaded, "registration", null));
        response.setMessage("Registration complete. Welcome!");
        log.info("instant registration complete for user: {}", users.getUsername());
        return response;
    }

    RegistrationInitResponse issueOtpChallenge(final Users users,
                                               final RegistrationInitRequest request) {
        final MfaFactorType selectedChannel = resolveRegistrationChannel(request.getVerificationChannel());
        final String destination = resolveDestinationFromRequest(request, selectedChannel);

        final OtpChallengeResponse challenge = otpChallengeService.issueChallenge(
                users, selectedChannel, OtpPurpose.REGISTRATION, destination);

        final RegistrationInitResponse response = new RegistrationInitResponse();
        response.setRequiresVerification(true);
        response.setChallengeId(challenge.getChallengeId());
        response.setChannel(challenge.getFactor());
        response.setMaskedDestination(challenge.getMaskedDestination());
        response.setExpiresInSeconds(challenge.getExpiresInSeconds());
        response.setMessage("Verification code sent. Please check your " + selectedChannel.name().toLowerCase());
        return response;
    }

    private MfaFactorType resolveRegistrationChannel(final MfaFactorType requested) {
        final Set<MfaFactorType> allowed = mfaPolicyResolver.allowedRegistrationChannels();
        final MfaFactorType selected = requested != null ? requested : mfaPolicyResolver.defaultRegistrationChannel();
        if (selected == MfaFactorType.TOTP || !allowed.contains(selected)) {
            throw new ResponseStatusException(BAD_REQUEST, "Registration verification channel is not allowed by policy");
        }
        return selected;
    }

    private String resolveDestinationFromRequest(final RegistrationInitRequest request,
            final MfaFactorType channel) {
        return switch (channel) {
            case EMAIL -> {
                if (request.getEmail() == null || request.getEmail().isBlank()) {
                    throw new ResponseStatusException(BAD_REQUEST, "Email is required for EMAIL verification");
                }
                yield request.getEmail();
            }
            case SMS -> {
                if (request.getPhone() == null || request.getPhone().isBlank()) {
                    throw new ResponseStatusException(BAD_REQUEST, "Phone is required for SMS verification");
                }
                yield request.getPhone();
            }
            default -> throw new ResponseStatusException(BAD_REQUEST, "Unsupported verification channel");
        };
    }

    private MfaFactorType resolveReissueChannel(final Users users) {
        final Set<MfaFactorType> allowed = mfaPolicyResolver.allowedRegistrationChannels();
        if (allowed.contains(MfaFactorType.EMAIL)
                && users.getEmail() != null && !users.getEmail().isBlank()) {
            return MfaFactorType.EMAIL;
        }
        if (allowed.contains(MfaFactorType.SMS)
                && users.getPhone() != null && !users.getPhone().isBlank()) {
            return MfaFactorType.SMS;
        }
        throw new ResponseStatusException(BAD_REQUEST,
                "No valid contact channel for re-verification. Please contact support.");
    }

    private String resolveDestinationForUser(final Users users, final MfaFactorType channel) {
        return switch (channel) {
            case EMAIL -> users.getEmail();
            case SMS -> users.getPhone();
            default -> throw new ResponseStatusException(BAD_REQUEST, "Unsupported re-verification channel");
        };
    }

    private AuthenticationResponse buildTokenResponse(final Users users) {
        final Users loaded = customUserDetailsService.loadUserByUsername(users.getUsername());
        final AuthenticationResponse response = new AuthenticationResponse();
        response.setAvailableRoles(loaded.getRole().stream()
                .map(Role::getName).sorted().toList());
        response.setUserId(loaded.getId());
        response.setAccessToken(jwtTokenService.generateAccessToken(loaded, "registration", null));
        response.setRefreshToken(jwtTokenService.generateRefreshToken(loaded, "registration", null));
        return response;
    }
}
