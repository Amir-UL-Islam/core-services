package com.central.security.core.security.oauth.controller;

import com.central.security.core.security.oauth.dto.AuthenticationResponse;
import com.central.security.core.security.oauth.dto.RegistrationInitRequest;
import com.central.security.core.security.oauth.dto.RegistrationInitResponse;
import com.central.security.core.security.oauth.dto.RegistrationVerifyRequest;
import com.central.security.core.security.oauth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "User Registration", description = "APIs for user registration and account management")
@RequestMapping("/api/v1")
public class UserRegistrationController {

    private final RegistrationService registrationService;

    public UserRegistrationController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Step 1 of registration.
     *
     * <ul>
     *   <li>When {@code registration.verification.required=false} (default): an account is enabled
     *       immediately and {@code accessToken}/{@code refreshToken} are returned.</li>
     *   <li>When {@code registration.verification.required=true}: an OTP is dispatched and
     *       {@code challengeId} is returned for use with {@code /register/verify}.</li>
     * </ul>
     */
    @PostMapping("register/init")
    @Operation(summary = "Initiate user registration")
    public ResponseEntity<RegistrationInitResponse> registerInit(
            @RequestBody @Valid final RegistrationInitRequest request) {
        return ResponseEntity.ok(registrationService.initRegistration(request));
    }

    /**
     * Step 2 of registration — only required when {@code requiresVerification=true} in the init response.
     *
     * <p>Verifies the OTP and activates the account. Returns tokens so no separate
     * {@code /authenticate} call is needed.
     */
    @PostMapping("register/verify")
    @Operation(summary = "Verify OTP and activate account")
    public ResponseEntity<AuthenticationResponse> registerVerify(
            @RequestBody @Valid final RegistrationVerifyRequest request) {
        return ResponseEntity.ok(registrationService.verifyRegistration(request));
    }
}
