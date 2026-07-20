package com.central.security.core.security.audit.repository;

import com.central.security.core.security.audit.model.entity.PolicyChangeAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PolicyChangeAuditRepository extends JpaRepository<PolicyChangeAudit, Long> {

    /**
     * Find all changes for a specific policy.
     */
    @Query("SELECT p FROM PolicyChangeAudit p WHERE p.policyType = :policyType AND p.policyId = :policyId")
    Page<PolicyChangeAudit> findByPolicy(PolicyChangeAudit.PolicyType policyType, Long policyId, Pageable pageable);

    /**
     * Find all deletions of a policy type (useful for detecting accidental deletions).
     */
    @Query("SELECT p FROM PolicyChangeAudit p WHERE p.policyType = :policyType AND p.changeType = 'DELETE'")
    Page<PolicyChangeAudit> findDeletions(PolicyChangeAudit.PolicyType policyType, Pageable pageable);

    /**
     * Find changes made by a specific user/admin.
     */
    Page<PolicyChangeAudit> findByChangedByUserId(Long userId, Pageable pageable);

    /**
     * Find all changes in a time range (for incident investigation).
     */
    @Query("SELECT p FROM PolicyChangeAudit p WHERE p.createDate >= :startTime AND p.createDate <= :endTime")
    List<PolicyChangeAudit> findChangesInTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find changes of a specific type.
     */
    Page<PolicyChangeAudit> findByChangeType(PolicyChangeAudit.ChangeType changeType, Pageable pageable);
}

