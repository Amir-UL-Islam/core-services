package com.central.security.core.security.oauth.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerificationRequest {
    @Size(max = 128)
    private String challengeId;

    @Size(min = 4, max = 10)
    private String otp;
}
