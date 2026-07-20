package com.central.security.core.security.sod.model.entity;

import com.problemfighter.java.base.entity.BaseUserEntity;
import com.problemfighter.pfspring.restapi.inter.model.RestEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Separation-of-Duties (SoD) constraint between two mutually-exclusive roles.
 * <p>
 * An SoD constraint records that a user MUST NOT hold both {@code roleNameA}
 * and {@code roleNameB} simultaneously.  This is the static SoD model required
 * by RBAC3 (NIST SP 800-192).
 * <p>
 * Constraints are seeded by {@link com.central.security.core.security.sod.SodConstraintLoader}
 * on startup and can also be managed via the admin API.
 */
@Entity
@Table(
    name = "sod_constraint",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sod_role_pair",
        columnNames = {"role_name_a", "role_name_b"}
    )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SodConstraint extends BaseUserEntity implements RestEntity {

    /**
     * First role in the mutually-exclusive pair.
     * Stored in canonical UPPER_SNAKE_CASE (e.g. {@code ADMIN}).
     */
    @Column(name = "role_name_a", nullable = false)
    private String roleNameA;

    /**
     * Second role in the mutually-exclusive pair (must differ from {@code roleNameA}).
     */
    @Column(name = "role_name_b", nullable = false)
    private String roleNameB;

    /** Human-readable description of why these roles conflict. */
    @Column(length = 500)
    private String reason;

    /**
     * Static SoD (dynamic=false, default): the two roles can NEVER be co-assigned to a user.
     * Dynamic SoD (dynamic=true): the user may hold both roles but cannot activate both
     * simultaneously within the same JWT session (NIST RBAC3 dynamic SoD model).
     */
    @Column(nullable = false)
    private boolean dynamic = false;

    /**
     * Returns {@code true} when the two supplied roles conflict under this constraint.
     * Comparison is order-independent and case-insensitive.
     */
    public boolean conflicts(String roleA, String roleB) {
        return (roleNameA.equalsIgnoreCase(roleA) && roleNameB.equalsIgnoreCase(roleB))
                || (roleNameA.equalsIgnoreCase(roleB) && roleNameB.equalsIgnoreCase(roleA));
    }
}

