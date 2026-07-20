package com.central.security.core.security.oauth.dto;

import com.central.security.core.security.validation.SecurityValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhoneNumberVerificationRequest extends OtpVerificationRequest {
    @Size(max = 32)
    @Pattern(regexp = SecurityValidationPatterns.BD_IN_CONTACT_REGEX, message = "Phone must be a valid Bangladeshi or Indian number")
    private String phone;
}
