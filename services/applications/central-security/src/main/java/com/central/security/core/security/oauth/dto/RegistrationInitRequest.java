package com.central.security.core.security.oauth.dto;

import com.central.security.core.security.annotation.GenderValue;
import com.central.security.core.security.mfa.MfaFactorType;
import com.central.security.core.security.validation.SecurityValidationPatterns;
import com.central.security.core.users.UsersUsernameUnique;
import com.central.security.core.users.model.Relation;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationInitRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    @Email(message = "Email must be a valid address")
    private String email;

    @Size(max = 32)
    @Pattern(regexp = SecurityValidationPatterns.BD_IN_CONTACT_REGEX,
            message = "Phone must be a valid Bangladeshi or Indian number")
    private String phone;

    @GenderValue
    private String gender;

    private Relation relation;

    @NotBlank
    @Size(max = 255)
    @UsersUsernameUnique(message = "{registration.register.taken}", unverifiedMessage = "{registration.register.unverified}")
    private String username;

    @NotBlank
    // @Size(min = 8, max = 72)
    private String password;

    private MfaFactorType verificationChannel;

    @AssertTrue(message = "verificationChannel EMAIL requires email, SMS requires phone")
    public boolean isVerificationDestinationValid() {
        if (verificationChannel == null || verificationChannel == MfaFactorType.TOTP) {
            return true;
        }
        if (verificationChannel == MfaFactorType.EMAIL) {
            return email != null && !email.isBlank();
        }
        return phone != null && !phone.isBlank();
    }
}

