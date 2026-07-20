package com.central.security.core.security.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.central.security.core.security.audit.model.entity.PolicyChangeAudit;
import com.central.security.core.security.audit.repository.PolicyChangeAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for recording and querying policy change audit trail.
 * Tracks all modifications to access control policies for compliance and incident investigation.
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class PolicyChangeAuditService {

    private final ObjectMapper objectMapper;
    private final PolicyChangeAuditRepository auditRepository;

    /**
     * Record a policy change.
     *
     * @param policyType The type of policy (ROLE, PRIVILEGE, URL_ENDPOINT, etc.)
     * @param policyId The ID of the policy entity
     * @param changeType The type of change (CREATE, UPDATE, DELETE, ASSIGN, UNASSIGN)
     * @param changedByUserId The user who made the change (null if system-initiated)
     * @param changedByUsername The username of who made the change
     * @param previousState The previous state of the policy (serialized)
     * @param newState The new state of the policy (serialized)
     * @param reason Human-readable reason for the change
     */
    public void recordChange(PolicyChangeAudit.PolicyType policyType, Long policyId,
                           PolicyChangeAudit.ChangeType changeType, Long changedByUserId,
                           String changedByUsername, Object previousState, Object newState,
                           String reason) {
        try {
            PolicyChangeAudit audit = PolicyChangeAudit.builder()
                    .policyType(policyType)
                    .policyId(policyId)
                    .changeType(changeType)
                    .changedByUserId(changedByUserId)
                    .changedByUsername(changedByUsername)
                    .previousState(serializeState(previousState))
                    .newState(serializeState(newState))
                    .reason(reason)
                    .build();

            auditRepository.save(audit);
            log.info("Recorded policy change: {} {} by {}", policyType, changeType, changedByUsername);
        } catch (Exception e) {
            log.error("Failed to record policy change audit", e);
            // Don't throw - audit failure should not block operation
        }
    }

    /**
     * Get audit trail for a specific policy.
     */
    public Page<PolicyChangeAudit> getAuditTrailForPolicy(PolicyChangeAudit.PolicyType policyType,
                                                         Long policyId, Pageable pageable) {
        return auditRepository.findByPolicy(policyType, policyId, pageable);
    }

    /**
     * Get all deletions of a policy type (for detecting accidental removals).
     */
    public Page<PolicyChangeAudit> getDeletionsForPolicyType(PolicyChangeAudit.PolicyType policyType,
                                                            Pageable pageable) {
        return auditRepository.findDeletions(policyType, pageable);
    }

    /**
     * Get all changes made by a specific admin user.
     */
    public Page<PolicyChangeAudit> getChangesByUser(Long userId, Pageable pageable) {
        return auditRepository.findByChangedByUserId(userId, pageable);
    }

    /**
     * Get all policy changes in a time range (for incident investigation).
     */
    public List<PolicyChangeAudit> getChangesInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditRepository.findChangesInTimeRange(startTime, endTime);
    }

    /**
     * Get all changes of a specific type (e.g., all DELETIONS).
     */
    public Page<PolicyChangeAudit> getChangesByType(PolicyChangeAudit.ChangeType changeType, Pageable pageable) {
        return auditRepository.findByChangeType(changeType, pageable);
    }

    /**
     * Serialize state object to JSON string.
     */
    private String serializeState(Object state) {
        if (state == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            log.warn("Failed to serialize state object", e);
            return state.toString();
        }
    }
}

