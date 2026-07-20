package com.central.security.core.security.policy.config;

public final class SecurityPolicyKeys {

    private SecurityPolicyKeys() {
    }

    /**
     * When {@code false} (default): registration completes immediately — account is
     * enabled on {@code POST /register/init} and tokens are returned in the response.
     * When {@code true}: an OTP challenge is issued and the caller must complete
     * {@code POST /register/verify} to activate the account.
     */
    public static final String REGISTRATION_VERIFICATION_REQUIRED = "registration.verification.required";

    public static final String REGISTRATION_ALLOWED_CHANNELS = "registration.allowed.channels";
    public static final String REGISTRATION_DEFAULT_CHANNEL = "registration.default.channel";
    public static final String MFA_OPTIONAL_PER_USER = "mfa.optional.per.user";
    public static final String MFA_ENFORCED_ROLES = "mfa.enforced.roles";
    public static final String MFA_ALLOWED_FACTORS_USER = "mfa.allowed.factors.user";
    public static final String MFA_ALLOWED_FACTORS_ADMIN = "mfa.allowed.factors.admin";
    public static final String MFA_ALLOWED_FACTORS_SUPER_ADMIN = "mfa.allowed.factors.super_admin";

    public static final String NOTIFICATION_DEFAULT_CHANNELS = "notification.default.channels";
    public static final String NOTIFICATION_BED_RESERVATION_CHANNELS = "notification.bed_reservation.channels";
    public static final String NOTIFICATION_REFERRAL_CHANNELS = "notification.referral.channels";
    public static final String NOTIFICATION_SMS_CONTENT_LANGUAGE = "notification.sms.content.language";
}

