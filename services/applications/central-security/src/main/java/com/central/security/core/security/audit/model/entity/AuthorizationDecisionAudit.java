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
 * Audit trail for authorization decisions.
 * Records every access attempt (allow/deny) for security monitoring and compliance.
 */
@Entity
@Table(
    name = "authorization_decision_audit",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_decision", columnList = "decision"),
        @Index(name = "idx_created_at", columnList = "create_date"),
        @Index(name = "idx_endpoint_method", columnList = "endpoint, method")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationDecisionAudit extends BaseUserEntity implements RestEntity {

    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthorizationDecision decision;

    /**
     * Comma-separated list of authorities the user had at the time of decision.
     * Stored for forensic analysis if policies are later changed.
     */
    @Column(length = 4000)
    private String userAuthorities;

    /**
     * Reason for denial, if the decision was DENY.
     * Examples: "No policy defined", "Required privilege not found", "User disabled"
     */
    @Column(length = 500)
    private String denialReason;

    public enum AuthorizationDecision {
        ALLOW, DENY
    }
}

