package com.central.security.core.security.oauth.dto;

import com.central.security.core.security.mfa.MfaFactorType;
import lombok.Getter;
import lombok.Setter;

/**
 * Response for {@code POST /register/init}.
 *
 * <p>The shape depends on the active registration policy:
 * <ul>
 *   <li><b>No verification required</b> ({@code requiresVerification=false}):
 *       Account is immediately active. {@code accessToken} and {@code refreshToken}
 *       are populated — the client can start using the API right away.</li>
 *   <li><b>Verification required</b> ({@code requiresVerification=true}):
 *       An OTP was dispatched via {@code channel}. The client must call
 *       {@code POST /register/verify} with {@code challengeId} + OTP to activate
 *       the account and receive tokens.</li>
 * </ul>
 */
@Getter
@Setter
public class RegistrationInitResponse {
    private Long userId;

    /**
     * True when the account is NOT yet active and OTP verification is required.
     */
    private boolean requiresVerification;

    // -----------------------------------------------------------------------
    // Present when requiresVerification=false (instant registration)
    // -----------------------------------------------------------------------

    private String accessToken;
    private String refreshToken;

    // -----------------------------------------------------------------------
    // Present when requiresVerification=true (OTP challenge)
    // -----------------------------------------------------------------------

    private String challengeId;
    private MfaFactorType channel;
    private String maskedDestination;
    private long expiresInSeconds;

    // -----------------------------------------------------------------------
    // Always present
    // -----------------------------------------------------------------------

    private String message;
}
