package com.central.security.core.security.oauth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequest extends OtpVerificationRequest {

    @Size(max = 255)
    @Email(message = "Email must be a valid address")
    private String email;

}
