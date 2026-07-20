package com.central.security.core.security.oauth;


import java.util.Set;
import java.util.regex.Pattern;


/**
 * Canonical permission-code registry.
 *
 * <h3>Naming convention</h3>
 * All fine-grained permissions follow the {@code resource:action} or the extended
 * {@code resource:action:scope} (RBAC3 parameterized) format:
 * <pre>
 *   ambulance:read          – read any ambulance record
 *   ambulance:read:own      – read only records the caller owns
 *   report:read:unit=ICU    – read reports scoped to the ICU ward
 * </pre>
 * Each segment matches {@code [a-z][a-z0-9_-]*}.
 *
 * <h3>Legacy codes</h3>
 * {@code ADMIN} and {@code USER} exist for backwards compatibility.
 * They are permitted but should not be used for new endpoints — prefer fine-grained
 * {@code resource:action} codes instead.  See {@link #isLegacyCode(String)}.
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    // ------------------------------------------------------------------
    // Legacy compatibility codes (kept for existing URL ACL seeders)
    // New endpoints MUST use resource:action codes below.
    // ------------------------------------------------------------------
    public static final String ADMIN = "ADMIN";
    public static final String USER  = "USER";

    // User management
    public static final String USER_READ   = "user:read";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";

    // Role management
    public static final String ROLE_READ   = "role:read";
    public static final String ROLE_CREATE = "role:create";
    public static final String ROLE_UPDATE = "role:update";
    public static final String ROLE_DELETE = "role:delete";

    // Privilege management
    public static final String PRIVILEGE_READ   = "privilege:read";
    public static final String PRIVILEGE_CREATE = "privilege:create";
    public static final String PRIVILEGE_UPDATE = "privilege:update";
    public static final String PRIVILEGE_DELETE = "privilege:delete";
    public static final String PRIVILEGE_ASSIGN = "privilege:assign";

    // URL endpoint management
    public static final String URL_READ   = "url:read";
    public static final String URL_CREATE = "url:create";
    public static final String URL_UPDATE = "url:update";
    public static final String URL_DELETE = "url:delete";

    // Permission matrix (ACL inspection)
    public static final String MATRIX_READ   = "matrix:read";
    public static final String MATRIX_MANAGE = "matrix:manage";

    // Security policy
    public static final String POLICY_READ   = "policy:read";
    public static final String POLICY_MANAGE = "policy:manage";

    // MFA
    public static final String MFA_MANAGE    = "mfa:manage";
    public static final String MFA_CHALLENGE = "mfa:challenge";
    public static final String MFA_VERIFY    = "mfa:verify";

    // Verification
    public static final String VERIFICATION_PHONE_REQUEST = "verification:phone:request";
    public static final String VERIFICATION_PHONE_VERIFY = "verification:phone:verify";
    public static final String VERIFICATION_EMAIL_REQUEST = "verification:email:request";
    public static final String VERIFICATION_EMAIL_VERIFY = "verification:email:verify";

    // Module management
    public static final String MODULE_READ   = "module:read";
    public static final String MODULE_MANAGE = "module:manage";

    // Dashboard Domain
    public static final String DASHBOARD_READ   = "dashboard:read";

    // Hospital domain
    public static final String HOSPITAL_READ       = "hospital:read";
    public static final String HOSPITAL_CREATE     = "hospital:create";
    public static final String HOSPITAL_UPDATE     = "hospital:update";
    public static final String HOSPITAL_DELETE     = "hospital:delete";
    public static final String HOSPITAL_READ_OWN   = "hospital:read:own";
    public static final String HOSPITAL_UPDATE_OWN = "hospital:update:own";

    // Admission domain
    public static final String ADMISSION_READ       = "admission:read";
    public static final String ADMISSION_CREATE     = "admission:create";
    public static final String ADMISSION_UPDATE     = "admission:update";
    public static final String ADMISSION_READ_OWN   = "admission:read:own";

    // Ambulance domain
    public static final String AMBULANCE_READ   = "ambulance:read";
    public static final String AMBULANCE_CREATE = "ambulance:create";
    public static final String AMBULANCE_UPDATE = "ambulance:update";
    public static final String AMBULANCE_DELETE = "ambulance:delete";
    public static final String AMBULANCE_UPDATE_OWN = "ambulance:update:own";

    // Admin registration
    public static final String ADMIN_REGISTER_HOSPITAL    = "admin:register:hospital";
    public static final String ADMIN_REGISTER_AMBULANCE   = "admin:register:ambulance";
    public static final String ADMIN_REGISTER_NICU_ADMIN  = "admin:register:nicu-admin";

    // Referral domain
    public static final String REFERRAL_READ     = "referral:read";
    public static final String REFERRAL_CREATE   = "referral:create";
    public static final String REFERRAL_MANAGE   = "referral:manage";   // accept / reject
    public static final String REFERRAL_READ_OWN = "referral:read:own"; // own hospital's referrals only

    // SoD constraint management
    public static final String SOD_READ   = "sod:read";
    public static final String SOD_MANAGE = "sod:manage";

    // Role dependency management
    public static final String ROLE_DEPENDENCY_READ   = "role:dependency:read";
    public static final String ROLE_DEPENDENCY_MANAGE = "role:dependency:manage";

    // Role hierarchy management
    public static final String HIERARCHY_READ   = "hierarchy:read";
    public static final String HIERARCHY_MANAGE = "hierarchy:manage";

    // Active-role context switching (any authenticated user on their own roles)
    public static final String CONTEXT_SWITCH = "context:switch";

    // ------------------------------------------------------------------
    // Scoped (parameterized) permission codes — RBAC3 §4 parameterized roles
    // These allow the same action to be granted at different resource scopes
    // without introducing new roles.
    // ------------------------------------------------------------------
    /** Read only ambulance records assigned to / owned by the caller. */
    public static final String AMBULANCE_READ_OWN  = "ambulance:read:own";
    /** Create ambulance records within the caller's assigned unit. */
    public static final String AMBULANCE_CREATE_UNIT = "ambulance:create:unit";
    /** Read user records scoped to the caller's department. */
    public static final String USER_READ_OWN       = "user:read:own";
    /** Update own user profile only. */
    public static final String USER_UPDATE_OWN     = "user:update:own";

    // ------------------------------------------------------------------
    // Validation patterns
    // ------------------------------------------------------------------

    /**
     * Matches the canonical {@code resource:action} and
     * {@code resource:action:scope} formats.
     *
     * Grammar:  {@code [a-z][a-z0-9_-]*:[a-z][a-z0-9_-]*(:[a-z0-9_=.-]+)?}
     *
     * Examples:
     * <ul>
     *   <li>{@code user:read} — flat permission</li>
     *   <li>{@code ambulance:read:own} — scoped permission</li>
     *   <li>{@code report:read:unit=ICU} — parameterized scope</li>
     * </ul>
     */
    private static final Pattern MATRIX_PERMISSION_PATTERN =
            Pattern.compile("^[a-z][a-z0-9_-]*:[a-z][a-z0-9_-]*(:[a-z0-9_=.-]+)?$");

    private static final Set<String> KNOWN_CODES = Set.of(
            ADMIN,
            USER,
            USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE,
            ROLE_READ, ROLE_CREATE, ROLE_UPDATE, ROLE_DELETE,
            PRIVILEGE_READ, PRIVILEGE_CREATE, PRIVILEGE_UPDATE, PRIVILEGE_DELETE, PRIVILEGE_ASSIGN,
            URL_READ, URL_CREATE, URL_UPDATE, URL_DELETE,
            MATRIX_READ, MATRIX_MANAGE,
            POLICY_READ, POLICY_MANAGE,
            SOD_READ, SOD_MANAGE,
            ROLE_DEPENDENCY_READ, ROLE_DEPENDENCY_MANAGE,
            HIERARCHY_READ, HIERARCHY_MANAGE,
            CONTEXT_SWITCH,
            MFA_MANAGE, MFA_CHALLENGE, MFA_VERIFY,
            VERIFICATION_PHONE_REQUEST, VERIFICATION_PHONE_VERIFY,
            VERIFICATION_EMAIL_REQUEST, VERIFICATION_EMAIL_VERIFY,
            MODULE_READ, MODULE_MANAGE,
            HOSPITAL_READ, HOSPITAL_CREATE, HOSPITAL_UPDATE, HOSPITAL_DELETE,
            HOSPITAL_READ_OWN, HOSPITAL_UPDATE_OWN,
            ADMISSION_READ, ADMISSION_CREATE, ADMISSION_UPDATE, ADMISSION_READ_OWN,
            AMBULANCE_READ, AMBULANCE_CREATE, AMBULANCE_UPDATE, AMBULANCE_DELETE,
            AMBULANCE_READ_OWN, AMBULANCE_CREATE_UNIT, AMBULANCE_UPDATE_OWN,
            ADMIN_REGISTER_HOSPITAL, ADMIN_REGISTER_AMBULANCE, ADMIN_REGISTER_NICU_ADMIN,
            REFERRAL_READ, REFERRAL_CREATE, REFERRAL_MANAGE, REFERRAL_READ_OWN,
            USER_READ_OWN, USER_UPDATE_OWN, DASHBOARD_READ
    );

    /** True if the code is in the canonical registry. */
    public static boolean isKnownCode(final String value) {
        return value != null && KNOWN_CODES.contains(value);
    }

    /**
     * True if the code is a legacy flat-role name ({@code ADMIN} / {@code USER}).
     * These are tolerated for backwards compatibility but should not be used in
     * new endpoint policy definitions.
     */
    public static boolean isLegacyCode(final String value) {
        return ADMIN.equals(value) || USER.equals(value);
    }

    /**
     * True if the code follows the {@code resource:action} or
     * {@code resource:action:scope} naming convention.
     */
    public static boolean isMatrixCode(final String value) {
        return value != null && MATRIX_PERMISSION_PATTERN.matcher(value).matches();
    }

    /**
     * True if the code is a supported permission (either legacy or matrix format).
     * Used by {@link com.central.security.core.privilege.PermissionCodeFormat} validator.
     */
    public static boolean isSupportedPermissionCode(final String value) {
        return isLegacyCode(value) || isMatrixCode(value);
    }
}

