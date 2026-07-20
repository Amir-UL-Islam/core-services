package com.central.security.core.security.oauth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class AuthenticationRequest {

    @NotNull
    @Size(max = 255)
    private String username;

    @NotNull
    @Size(max = 72)
    private String password;

    // Optional TOTP code when two-factor is enabled
    private String otp;

    // Optional MFA channel hint: TOTP, SMS, EMAIL
    private String otpChannel;

    // Required for SMS/EMAIL challenge verification in login second step
    private String challengeId;

    /**
     * Optional: Role activation for Dynamic SoD (NIST RBAC3).
     * When provided, only these roles are included in the issued JWT.
     * The system validates:
     *   1. User must actually hold all requested roles.
     *   2. No dynamic SoD constraint may be violated by activating these roles simultaneously.
     * If null/empty, all the user's assigned roles are activated (default behavior).
     */
    private List<String> activeRoles;

}
