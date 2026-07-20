
package com.central.security.core.security.oauth.controller;

import com.problemfighter.pfspring.restapi.rr.RequestResponse;
import com.problemfighter.pfspring.restapi.rr.Utility;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.central.security.core.security.mfa.otp.dto.OtpChallengeResponse;
import com.central.security.core.security.oauth.dto.ForgotPasswordRequest;
import com.central.security.core.security.oauth.dto.ResetPasswordRequest;
import com.central.security.core.security.oauth.service.ForgotPasswordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Forgot Password", description = "APIs for password reset functionality")
public class ForgotPasswordController implements RequestResponse {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset",
            description = "Sends an OTP to the user's verified email or phone for password reset")
    public DetailsResponse<OtpChallengeResponse> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        OtpChallengeResponse response = forgotPasswordService.requestPasswordReset(request.getUsername());
        return Utility.response(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password",
            description = "Verifies OTP and resets the user's password")
    public MessageResponse resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        forgotPasswordService.resetPassword(
                request.getChallengeId(),
                request.getOtp(),
                request.getNewPassword(),
                request.getUsername()
        );
        return responseProcessor().response("Password has been reset successfully");
    }
}
