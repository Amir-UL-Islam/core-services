package com.central.security.core.security.audit.model.entity;

import com.problemfighter.java.base.entity.BaseUserEntity;
import com.problemfighter.pfspring.restapi.inter.model.RestEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Audit trail for policy/permission configuration changes.
 * Records all modifications to Roles, Privileges, and URL endpoints.
 * Used for compliance and investigating access control policy drift.
 */
@Entity
@Table(
    name = "policy_change_audit",
    indexes = {
        @Index(name = "idx_policy_type", columnList = "policy_type"),
        @Index(name = "idx_policy_id", columnList = "policy_id"),
        @Index(name = "idx_change_type", columnList = "change_type"),
        @Index(name = "idx_created_at", columnList = "create_date")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyChangeAudit extends BaseUserEntity implements RestEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType policyType;

    @Column(nullable = false)
    private Long policyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    /**
     * User who made the change (admin/operator).
     * Null if system-initiated change.
     */
    @Column
    private Long changedByUserId;

    @Column
    private String changedByUsername;

    /**
     * Serialized previous state (JSON).
     * For investigation if a policy change had unintended consequences.
     */
    @Column(columnDefinition = "TEXT")
    private String previousState;

    /**
     * Serialized new state (JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String newState;

    /**
     * Reason for the change (from API request).
     */
    @Column(length = 500)
    private String reason;

    public enum PolicyType {
        ROLE, PRIVILEGE, URL_ENDPOINT, ROLE_PRIVILEGE_ASSIGNMENT, USER_ROLE_ASSIGNMENT
    }

    public enum ChangeType {
        CREATE, UPDATE, DELETE, ASSIGN, UNASSIGN
    }
}

