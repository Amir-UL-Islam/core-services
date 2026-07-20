package com.central.security.core.role.model.entity;

import com.problemfighter.java.base.entity.BaseUserEntity;
import com.problemfighter.pfspring.restapi.inter.model.RestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents a prerequisite relationship between two roles.
 *
 * <p>When a user is assigned {@code dependentRole}, they MUST also hold
 * {@code requiredRole} in the same role set. Enforcement is bidirectional:
 * removing {@code requiredRole} while the user still holds {@code dependentRole}
 * is also rejected.
 *
 * <p>Managed by {@link com.central.security.core.role.service.RoleDependencyService}
 * and enforced at assignment time via
 * {@link com.central.security.core.users.model.mapper.UsersInterceptor}.
 */
@Entity
@Table(
        name = "role_dependency",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_dependency_pair",
                columnNames = {"dependent_role_id", "required_role_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class RoleDependency extends BaseUserEntity implements RestEntity {

    /** The role whose assignment requires {@code requiredRole} to also be present. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_role_id", nullable = false)
    private Role dependentRole;

    /** The role that must be present whenever {@code dependentRole} is assigned. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "required_role_id", nullable = false)
    private Role requiredRole;

    /** Human-readable explanation of why this dependency exists. */
    @Column(length = 500)
    private String reason;
}
