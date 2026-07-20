package com.central.security.core.security.audit.service;

import com.central.security.core.security.audit.model.entity.AuthorizationDecisionAudit;
import com.central.security.core.security.audit.repository.AuthorizationDecisionAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for recording and querying authorization decision audit trail.
 * Implements compliance requirements for access logging and forensic investigation.
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class AuthorizationDecisionAuditService {

    private final AuthorizationDecisionAuditRepository auditRepository;

    /**
     * Record an authorization decision (allow or deny).
     *
     * @param userId The user's ID (or null if unauthenticated)
     * @param username The user's username (or "anonymous" if unauthenticated)
     * @param endpoint The request endpoint
     * @param method The HTTP method
     * @param decision Whether access was allowed or denied
     * @param authentication The Spring Security authentication (for extracting authorities)
     * @param denialReason If the decision is DENY, the reason why
     */
    public void recordDecision(Long userId, String username, String endpoint, String method,
                             AuthorizationDecisionAudit.AuthorizationDecision decision,
                             Authentication authentication, String denialReason) {
        try {
            String authorities = extractAuthoritiesString(authentication);

            AuthorizationDecisionAudit audit = AuthorizationDecisionAudit.builder()
                    .userId(userId)
                    .username(username != null ? username : "anonymous")
                    .endpoint(endpoint)
                    .method(method)
                    .decision(decision)
                    .userAuthorities(authorities)
                    .denialReason(denialReason)
                    .build();

            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to record authorization decision audit", e);
            // Don't throw - audit failure should not block request
        }
    }

    /**
     * Record a denied authorization decision with reason.
     */
    public void recordDenied(Long userId, String username, String endpoint, String method,
                            Authentication authentication, String reason) {
        recordDecision(userId, username, endpoint, method,
                AuthorizationDecisionAudit.AuthorizationDecision.DENY, authentication, reason);
    }

    /**
     * Record an allowed authorization decision.
     */
    public void recordAllowed(Long userId, String username, String endpoint, String method,
                             Authentication authentication) {
        recordDecision(userId, username, endpoint, method,
                AuthorizationDecisionAudit.AuthorizationDecision.ALLOW, authentication, null);
    }

    /**
     * Get audit records for a specific user.
     */
    public Page<AuthorizationDecisionAudit> getAuditTrailForUser(Long userId, Pageable pageable) {
        return auditRepository.findByUserId(userId, pageable);
    }

    /**
     * Get all denied access attempts.
     */
    public Page<AuthorizationDecisionAudit> getDeniedAttempts(Pageable pageable) {
        return auditRepository.findByDecision(
                AuthorizationDecisionAudit.AuthorizationDecision.DENY, pageable);
    }

    /**
     * Get denied attempts for a specific user.
     */
    public Page<AuthorizationDecisionAudit> getDeniedAttemptsForUser(Long userId, Pageable pageable) {
        return auditRepository.findByUserIdAndDecision(userId,
                AuthorizationDecisionAudit.AuthorizationDecision.DENY, pageable);
    }

    /**
     * Get audit records for a specific endpoint.
     */
    public Page<AuthorizationDecisionAudit> getAuditTrailForEndpoint(String endpoint, String method,
                                                                     Pageable pageable) {
        return auditRepository.findByEndpointAndMethod(endpoint, method, pageable);
    }

    /**
     * Get denied attempts in a time range (for incident investigation).
     */
    public List<AuthorizationDecisionAudit> getDeniedAttemptsInTimeRange(LocalDateTime startTime,
                                                                         LocalDateTime endTime) {
        return auditRepository.findDeniedInTimeRange(
                AuthorizationDecisionAudit.AuthorizationDecision.DENY, startTime, endTime);
    }

    /**
     * Extract authorities from Spring Security Authentication for audit logging.
     */
    private String extractAuthoritiesString(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}

