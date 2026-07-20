package com.central.security.core.security.oauth.controller;

import com.central.security.core.security.jwt.JwtTokenService;
import com.central.security.core.security.oauth.dto.AuthenticationResponse;
import com.central.security.core.security.oauth.dto.SwitchContextRequest;
import com.central.security.core.security.sod.SodConstraintService;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dynamic SoD role-activation endpoint (NIST RBAC3 — Option B).
 *
 * POST /api/me/switch-context
 *   Allows an authenticated user to switch their active role context without re-logging-in.
 *   Returns a new access token scoped to the requested roles if:
 *     - The user holds all requested roles.
 *     - No dynamic SoD constraint is violated.
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Current User", description = "Current user context and role activation")
public class UserContextController {

    private final UsersRepository usersRepository;
    private final JwtTokenService jwtTokenService;
    private final SodConstraintService sodConstraintService;

    @PostMapping("/switch-context")
    @Transactional(readOnly = true)
    @Operation(summary = "Switch active role context (Dynamic SoD — NIST RBAC3 Option B)",
               description = "Re-issues an access token scoped to the requested subset of the " +
                             "user's assigned roles. Dynamic SoD constraints are enforced here.")
    public ResponseEntity<AuthenticationResponse> switchContext(
            final Authentication authentication,
            @RequestBody @Valid final SwitchContextRequest request) {

        final Users user = usersRepository.findByUsernameIgnoreCase(authentication.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        final List<String> requestedRoles = request.getActiveRoles();

        // Validate: user must actually hold every requested role
        final Set<String> userRoleNames = new HashSet<>();
        user.getRole().forEach(r -> userRoleNames.add(r.getName().toUpperCase()));

        for (String requested : requestedRoles) {
            if (!userRoleNames.contains(requested.toUpperCase())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "User does not hold role: " + requested);
            }
        }

        // Dynamic SoD check — validate the selected role combination
        sodConstraintService.assertNoDynamicConflict(requestedRoles);

        // Issue a new access token scoped to the requested active roles
        final Set<String> activeRoleSet = new HashSet<>(requestedRoles);
        final String newAccessToken = jwtTokenService.generateAccessTokenForActiveRoles(
                user, "direct", null, activeRoleSet);

        log.info("User {} switched context to roles: {}", user.getUsername(), requestedRoles);

        final AuthenticationResponse response = new AuthenticationResponse();
        response.setAccessToken(newAccessToken);
        response.setUserId(user.getId());
        response.setRefreshToken(null); // refresh token unchanged
        response.setRequiresMfa(false);
        return ResponseEntity.ok(response);
    }
}
