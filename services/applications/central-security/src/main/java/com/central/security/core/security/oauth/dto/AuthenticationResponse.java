package com.central.security.core.security.oauth.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;


/**
 * Unified response for all authentication endpoints.
 *
 * <h3>Cases</h3>
 * <dl>
 *   <dt>Successful login</dt>
 *   <dd>{@code accessToken}, {@code refreshToken}, and {@code availableRoles} are set.
 *       To start a session with a subset of roles (Dynamic SoD), re-authenticate with
 *       the desired {@code activeRoles} populated from {@code availableRoles}.</dd>
 *
 *   <dt>MFA required</dt>
 *   <dd>{@code requiresMfa=true}. Complete the challenge and re-authenticate.</dd>
 *
 *   <dt>Account not verified</dt>
 *   <dd>{@code requiresVerification=true}. A new OTP was dispatched. Submit it to
 *       {@code POST /register/verify} using {@code verificationChallengeId}.</dd>
 * </dl>
 */
@Getter
@Setter
public class AuthenticationResponse {
    private Long userId;

    // -----------------------------------------------------------------------
    // Happy-path: tokens
    // -----------------------------------------------------------------------

    private String accessToken;
    private String refreshToken;

    /**
     * All roles assigned to the authenticated user (sorted).
     * Use this to display a role-selection UI and re-authenticate with {@code activeRoles}
     * if Dynamic SoD role restriction is required for the session.
     */
    private List<String> availableRoles;

    // -----------------------------------------------------------------------
    // MFA challenge
    // -----------------------------------------------------------------------

    private boolean requiresMfa;
    private String mfaChallengeId;
    private String mfaChannel;
    private String mfaMessage;
    private long mfaExpiresInSeconds;

    // -----------------------------------------------------------------------
    // Account not yet verified (accountEnabled=false)
    // -----------------------------------------------------------------------

    /**
     * True when the account exists but has not completed email/SMS verification.
     */
    private boolean requiresVerification;

    /**
     * Challenge ID to use with {@code POST /register/verify}.
     */
    private String verificationChallengeId;

    /**
     * Channel through which the verification OTP was dispatched (EMAIL or SMS).
     */
    private String verificationChannel;

    private String verificationMessage;
    private long verificationExpiresInSeconds;
}
