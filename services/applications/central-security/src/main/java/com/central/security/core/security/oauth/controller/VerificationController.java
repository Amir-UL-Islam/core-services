package com.central.security.core.security.oauth.controller;

import com.problemfighter.pfspring.restapi.rr.RequestResponse;
import com.problemfighter.pfspring.restapi.rr.Utility;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.central.security.core.security.mfa.MfaFactorType;
import com.central.security.core.security.oauth.SecurityContext;
import com.central.security.core.security.oauth.dto.EmailVerificationRequest;
import com.central.security.core.security.oauth.dto.PhoneNumberVerificationRequest;
import com.central.security.core.security.mfa.otp.OtpChallengeService;
import com.central.security.core.security.mfa.otp.OtpPurpose;
import com.central.security.core.security.mfa.otp.dto.OtpChallengeResponse;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Verification")
public class VerificationController implements RequestResponse {

    private final UsersRepository usersRepository;
    private final OtpChallengeService otpChallengeService;

    @PostMapping("/request/phone-verification")
    @Operation(summary = "Request Phone Verification", description = "Request OTP for phone verification")
    public DetailsResponse<OtpChallengeResponse> requestPhoneVerification(
            @RequestBody @Valid PhoneNumberVerificationRequest verifyPhoneRequest
    ) {
        final Users user = SecurityContext.getCurrentLoggedUser();

        if (user.getPhoneVerified()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Phone number is already verified. Current verified phone number: " + user.getPhone());
        }

        if (user.getPhone() != null && !user.getPhone().equals(verifyPhoneRequest.getPhone())) {
            throw new ResponseStatusException(UNAUTHORIZED, "You are not getting OTP for Phone number that you do not register with.");
        }

        return Utility.response(otpChallengeService.issueChallenge(
                user,
                MfaFactorType.SMS,
                OtpPurpose.VERIFICATION,
                verifyPhoneRequest.getPhone()
        ));

    }

    @PatchMapping("/verify/phone")
    @Operation(summary = "Verify Phone", description = "Verify Phone. If phone number is missing in profile, then API will patch it.")
    public MessageResponse verifyPhone(
            @RequestBody @Valid PhoneNumberVerificationRequest request
    ) {
        final Users user = SecurityContext.getCurrentLoggedUser();
        final boolean isPhoneNumberExist = user.getPhone() != null || !user.getPhone().isBlank();

        otpChallengeService.verifyChallenge(
                request.getChallengeId(),
                user.getUsername(),
                OtpPurpose.VERIFICATION,
                request.getOtp()
        );
        user.setPhoneVerified(true);
        user.setPhone(request.getPhone());
        usersRepository.save(user);
        return Utility.response(isPhoneNumberExist ? "Phone Verified" : "Phone Number Added");
    }

    @PostMapping("/request/email-verification")
    @Operation(summary = "Request Email Verification", description = "Request OTP for email verification")
    public DetailsResponse<OtpChallengeResponse> requestEmailVerification(
            @RequestBody final EmailVerificationRequest request
    ) {
        final Users user = SecurityContext.getCurrentLoggedUser();
        if (user.getEmailVerified()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Email is already verified. Current verified mail: " + user.getEmail());
        }

        if (user.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            throw new ResponseStatusException(UNAUTHORIZED, "You are not getting OTP for Email that you do not register with.");
        }

        return Utility.response(otpChallengeService.issueChallenge(
                user,
                MfaFactorType.EMAIL,
                OtpPurpose.VERIFICATION,
                request.getEmail()
        ));

    }

    @PatchMapping("/verify/email")
    @Operation(summary = "Verify Email", description = "Verify Email. If email is missing in profile, then API will patch it.")
    public MessageResponse verifyEmail(
            @RequestBody @Valid EmailVerificationRequest request
    ) {
        final Users user = SecurityContext.getCurrentLoggedUser();
        final boolean isEmailExist = user.getEmail() != null || !user.getEmail().isBlank();
        otpChallengeService.verifyChallenge(
                request.getChallengeId(),
                user.getUsername(),
                OtpPurpose.VERIFICATION,
                request.getOtp()
        );
        user.setEmailVerified(true);
        user.setEmail(request.getEmail());
        usersRepository.save(user);
        return Utility.response(isEmailExist ? "Email Verified" : "Email Number Added");
    }
}
