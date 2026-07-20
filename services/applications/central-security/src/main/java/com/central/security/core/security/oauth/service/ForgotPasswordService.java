package com.central.security.core.security.oauth.service;

import com.central.security.core.security.mfa.MfaFactorType;
import com.central.security.core.security.mfa.otp.OtpChallenge;
import com.central.security.core.security.mfa.otp.OtpChallengeRepository;
import com.central.security.core.security.mfa.otp.OtpChallengeService;
import com.central.security.core.security.mfa.otp.OtpPurpose;
import com.central.security.core.security.mfa.otp.dto.OtpChallengeResponse;
import com.central.security.core.security.policy.config.SecurityPolicyKeys;
import com.central.security.core.security.policy.config.SecurityPolicyService;
import com.central.security.core.security.util.TokenVersionManager;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final OtpChallengeService otpChallengeService;
    private final TokenVersionManager tokenVersionManager;
    private final SecurityPolicyService securityPolicyService;
    private final OtpChallengeRepository otpChallengeRepository;

    @Transactional
    public OtpChallengeResponse requestPasswordReset(String username) {
        Users user = usersRepository.findByUsernameIgnoreCase(username);

        if (user == null) {
            log.warn("Password reset requested for non-existent user: {}", username);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        final boolean verificationRequired = securityPolicyService.getBooleanOrDefault(
                SecurityPolicyKeys.REGISTRATION_VERIFICATION_REQUIRED, false);
        log.info("Starting registration for user: {} (verificationRequired={})",
                username, verificationRequired);


        if (verificationRequired) {
            MfaFactorType factor = determineOtpFactor(user);
            String destination = resolveDestination(user, factor);
            return otpChallengeService.issueChallenge(
                    user,
                    factor,
                    OtpPurpose.PASSWORD_RESET,
                    destination
            );
        }
        return OtpChallengeResponse.builder()
                .factor(MfaFactorType.NONE)
                .message("No Otp verification needed.")
                .build();
    }

    @Transactional
    public void resetPassword(String challengeId, String otp, String newPassword, String username) {
        final boolean verificationRequired = securityPolicyService.getBooleanOrDefault(
                SecurityPolicyKeys.REGISTRATION_VERIFICATION_REQUIRED, false);
        if (verificationRequired) {
            OtpChallenge challenge = otpChallengeRepository.findByChallengeId(challengeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Invalid or expired challenge"));

            otpChallengeService.verifyChallenge(
                    challengeId,
                    challenge.getUsername(),
                    OtpPurpose.PASSWORD_RESET,
                    otp
            );

        }
        Users user = usersRepository.findByUsernameIgnoreCase(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        tokenVersionManager.invalidateAllTokens(user.getId());

        log.info("Password successfully reset for user: {}", user.getUsername());
    }

    private MfaFactorType determineOtpFactor(Users user) {
        if (Boolean.TRUE.equals(user.getEmailVerified()) && Boolean.TRUE.equals(user.getEmailMfaEnabled())) {
            return MfaFactorType.EMAIL;
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return MfaFactorType.EMAIL;
        }

        if (Boolean.TRUE.equals(user.getPhoneVerified()) && Boolean.TRUE.equals(user.getSmsMfaEnabled())) {
            return MfaFactorType.SMS;
        }

        if (Boolean.TRUE.equals(user.getPhoneVerified())) {
            return MfaFactorType.SMS;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No verified contact method available for OTP delivery");
    }

    private String resolveDestination(Users user, MfaFactorType factor) {
        return switch (factor) {
            case EMAIL -> {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email not configured");
                }
                yield user.getEmail();
            }
            case SMS -> {
                if (user.getPhone() == null || user.getPhone().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone not configured");
                }
                yield user.getPhone();
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported OTP factor: " + factor);
        };
    }
}

