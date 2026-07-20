package com.central.security.core.security.mfa;

import com.problemfighter.pfspring.restapi.rr.RequestResponse;
import com.problemfighter.pfspring.restapi.rr.Utility;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.central.security.core.security.mfa.dto.MfaSettingsRequest;
import com.central.security.core.security.mfa.dto.MfaSettingsResponse;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/mfa/settings")
@RequiredArgsConstructor
@Tag(name = "MFA Settings", description = "Manage authenticated user's MFA channel preferences")
public class MfaSettingsController implements RequestResponse {

    private final UsersRepository usersRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public DetailsResponse<MfaSettingsResponse> get(final Authentication authentication) {
        final Users user = usersRepository.findByUsernameIgnoreCase(authentication.getName());
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }

        return Utility.response(MfaSettingsResponse.builder()
                .totpEnabled(Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .smsEnabled(Boolean.TRUE.equals(user.getSmsMfaEnabled()))
                .emailEnabled(Boolean.TRUE.equals(user.getEmailMfaEnabled()))
                .phoneVerified(Boolean.TRUE.equals(user.getPhoneVerified()))
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .preferredFactor(user.getPreferredMfaFactor())
                .build());
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public MessageResponse update(final Authentication authentication,
                                  @RequestBody final MfaSettingsRequest request) {
        final Users user = usersRepository.findByUsernameIgnoreCase(authentication.getName());
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }

        if (Boolean.TRUE.equals(request.getSmsEnabled()) && !Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new ResponseStatusException(BAD_REQUEST, "Phone must be verified before enabling SMS MFA");
        }
        if (Boolean.TRUE.equals(request.getEmailEnabled()) && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(BAD_REQUEST, "Email must be verified before enabling EMAIL MFA");
        }

        if (request.getSmsEnabled() != null) {
            user.setSmsMfaEnabled(request.getSmsEnabled());
        }
        if (request.getEmailEnabled() != null) {
            user.setEmailMfaEnabled(request.getEmailEnabled());
        }
        if (request.getPreferredFactor() != null) {
            user.setPreferredMfaFactor(request.getPreferredFactor().name());
        }

        usersRepository.save(user);
        return Utility.response("MFA settings updated successfully");
    }
}

