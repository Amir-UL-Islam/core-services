package com.central.security.core.security.policy.config;

import com.central.security.core.security.policy.config.dto.SecurityPolicySettingDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/security/policy")
@RequiredArgsConstructor
@Tag(name = "Security Policy", description = "Runtime policy controls for registration and MFA")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    @GetMapping
    @PreAuthorize("hasAuthority('policy:read')")
    public ResponseEntity<Map<String, String>> getPolicy() {
        return ResponseEntity.ok(securityPolicyService.getPolicyMap());
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('policy:manage')")
    public ResponseEntity<Void> updatePolicy(@RequestBody @Valid final List<SecurityPolicySettingDTO> settings) {
        securityPolicyService.upsertAll(settings);
        return ResponseEntity.noContent().build();
    }
}

