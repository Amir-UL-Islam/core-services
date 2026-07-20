package com.central.security.core.security.mfa.otp.dto;

import com.central.security.core.security.mfa.MfaFactorType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OtpVerificationResult {
    private boolean verified;
    private MfaFactorType factor;
    private String username;
    private String destination;
    private String message;
}

