package com.central.security.core.security.policy.config;

import com.central.security.core.security.policy.config.dto.SecurityPolicySettingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Centralized manager for security policies and configuration.
 * Replaces hardcoded security parameters with database-driven configuration.
 *
 * This ensures:
 * - Single source of truth for security policies
 * - Runtime policy updates without redeployment
 * - Audit trail of policy changes
 * - Consistency across components
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CentralizedSecurityPolicyManager {

    private final SecurityPolicyService policyService;

    // Policy keys - standardized naming convention
    public static final String PUBLIC_PATHS_KEY = "security.public.paths";
    public static final String TOKEN_REVOCATION_ON_ROLE_CHANGE = "security.token.revoke.on.role.change";
    public static final String TOKEN_REVOCATION_ON_PRIVILEGE_CHANGE = "security.token.revoke.on.privilege.change";
    public static final String ENFORCE_TOKEN_FRESHNESS = "security.token.enforce.freshness";
    public static final String ADMIN_APPROVAL_REQUIRED = "security.admin.approval.required.for.role.change";

    /**
     * Get public/unauthenticated paths from database.
     * Falls back to sensible defaults if not configured.
     */
    public List<String> getPublicPaths() {
        String paths = policyService.getValueOrDefault(PUBLIC_PATHS_KEY,
                "/register,/authenticate,/authenticateGoogle,/refresh-token,/oauth/token");
        return policyService.getCsvOrDefault(PUBLIC_PATHS_KEY, paths);
    }

    /**
     * Check if tokens should be revoked when a user's role changes.
     * Enables immediate access revocation on role update.
     */
    public boolean shouldRevokeTokenOnRoleChange() {
        return policyService.getBooleanOrDefault(TOKEN_REVOCATION_ON_ROLE_CHANGE, true);
    }

    /**
     * Check if tokens should be revoked when a privilege changes.
     * Enables immediate access revocation on privilege modification.
     */
    public boolean shouldRevokeTokenOnPrivilegeChange() {
        return policyService.getBooleanOrDefault(TOKEN_REVOCATION_ON_PRIVILEGE_CHANGE, true);
    }

    /**
     * Check if token freshness is enforced.
     * If enabled, every request verifies token_version matches user's current version.
     */
    public boolean isTokenFreshnessEnforced() {
        return policyService.getBooleanOrDefault(ENFORCE_TOKEN_FRESHNESS, true);
    }

    /**
     * Check if admin approval is required for role changes.
     * Used for Separation of Duties enforcement.
     */
    public boolean isAdminApprovalRequired() {
        return policyService.getBooleanOrDefault(ADMIN_APPROVAL_REQUIRED, false);
    }

    /**
     * Get a custom policy value by key.
     */
    public Optional<String> getPolicy(String key) {
        return Optional.ofNullable(policyService.getValueOrDefault(key, null));
    }

    /**
     * Get a boolean policy value with default.
     */
    public boolean getBooleanPolicy(String key, boolean defaultValue) {
        return policyService.getBooleanOrDefault(key, defaultValue);
    }

    /**
     * Get a CSV-parsed policy value with default.
     */
    public List<String> getCsvPolicy(String key, String defaultValue) {
        return policyService.getCsvOrDefault(key, defaultValue);
    }

    /**
     * Update a policy value at runtime.
     * Note: Permissions should be strictly controlled to admin users only.
     */
    public void updatePolicy(String key, String value) {
        policyService.upsert(key, value);
        log.info("Updated security policy: {} = {}", key, value);
    }

    /**
     * Batch update multiple policies.
     * Useful for loading complete policy configurations.
     */
    public void updatePolicies(List<SecurityPolicySettingDTO> settings) {
        policyService.upsertAll(settings);
        log.info("Updated {} security policies", settings.size());
    }
}

