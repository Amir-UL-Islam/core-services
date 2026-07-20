package com.central.security.core.security.mfa.otp;

public enum OtpPurpose {
    REGISTRATION,
    LOGIN_MFA,
    ENTITY_REGISTRATION,   // for public hospital/ambulance self-registration flow
    PASSWORD_RESET,
    VERIFICATION
}

