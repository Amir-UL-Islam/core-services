package com.central.security.core.security.oauth.dto;

import com.central.security.core.security.annotation.UsernameEmailOrPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest extends OtpVerificationRequest{

    @NotBlank(message = "A valid username is required!")
    @Size(max = 255, message = "Username must be at most 255 characters")
    @UsernameEmailOrPhone(message = "Must be a valid username, email address, or phone number")
    private String username;

    @NotBlank(message = "New password is required")
    @Size(min = 4, max = 72, message = "Password must be between 4 and 72 characters")
    private String newPassword;
}
