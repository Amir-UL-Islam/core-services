package com.central.security.core.security.mfa.otp.provider;

import com.central.security.core.security.mfa.MfaFactorType;

public interface OtpDeliveryProvider {

    MfaFactorType factor();
    void sendOtp(String destination, String message);
}

