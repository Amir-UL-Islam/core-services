package com.central.security.core.security.audit.repository;

import com.central.security.core.security.audit.model.entity.AuthorizationDecisionAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuthorizationDecisionAuditRepository extends JpaRepository<AuthorizationDecisionAudit, Long> {

    /**
     * Find all audit records for a specific user.
     */
    Page<AuthorizationDecisionAudit> findByUserId(Long userId, Pageable pageable);

    /**
     * Find denied access attempts for a user.
     */
    Page<AuthorizationDecisionAudit> findByUserIdAndDecision(Long userId,
            AuthorizationDecisionAudit.AuthorizationDecision decision, Pageable pageable);

    /**
     * Find all denied access attempts (system-wide).
     */
    Page<AuthorizationDecisionAudit> findByDecision(AuthorizationDecisionAudit.AuthorizationDecision decision,
            Pageable pageable);

    /**
     * Find attempts on a specific endpoint.
     */
    @Query("SELECT a FROM AuthorizationDecisionAudit a WHERE a.endpoint = :endpoint AND a.method = :method")
    Page<AuthorizationDecisionAudit> findByEndpointAndMethod(String endpoint, String method, Pageable pageable);

    /**
     * Find denied attempts in a time range.
     */
    @Query("SELECT a FROM AuthorizationDecisionAudit a WHERE a.decision = :decision " +
           "AND a.createDate >= :startTime AND a.createDate <= :endTime")
    List<AuthorizationDecisionAudit> findDeniedInTimeRange(
            AuthorizationDecisionAudit.AuthorizationDecision decision,
            LocalDateTime startTime, LocalDateTime endTime);
}

