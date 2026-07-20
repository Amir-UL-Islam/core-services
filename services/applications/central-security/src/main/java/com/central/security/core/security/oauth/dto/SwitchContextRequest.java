package com.central.security.core.security.oauth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request to switch the caller's active role context without re-authenticating.
 * Implements NIST RBAC3 Dynamic SoD — Option B (context-switch endpoint).
 *
 * The server validates:
 *  1. The user actually holds every role in {@code activeRoles}.
 *  2. No dynamic SoD constraint is violated by activating all requested roles simultaneously.
 * If validation passes, a new access token scoped to the requested roles is returned.
 */
@Getter
@Setter
public class SwitchContextRequest {

    @NotEmpty(message = "activeRoles must not be empty")
    private List<String> activeRoles;
}
