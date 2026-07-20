package com.central.security.core.security.sod;

import com.central.security.core.security.sod.model.entity.SodConstraint;
import com.central.security.core.security.sod.repository.SodConstraintRepository;
import com.central.security.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Enforces Separation-of-Duties (SoD) constraints for RBAC3 compliance.
 *
 * Static SoD  (dynamic=false): role pairs can never be co-assigned to the same user.
 * Dynamic SoD (dynamic=true):  role pairs may be co-assigned but cannot both be
 *                               active in the same JWT session simultaneously.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SodConstraintService {

    private final SodConstraintRepository repository;

    // -----------------------------------------------------------------------
    // Static SoD — co-assignment enforcement
    // -----------------------------------------------------------------------

    /**
     * Assert that the complete set of roles does not contain any mutually-exclusive
     * STATIC pair. Throws {@link SodViolationException} (HTTP 422) on violation.
     * Only static constraints (dynamic=false) are checked here.
     */
    public void assertNoPairConflict(Collection<String> roles) {
        final List<SodConstraint> constraints = repository.findAllByDynamicFalse();
        checkConflicts(roles, constraints);
    }

    /**
     * Assert that adding {@code incomingRoles} to a user who already holds
     * {@code existingRoles} does not violate any static SoD constraint.
     */
    public void assertNoConflict(Collection<String> incomingRoles, Collection<String> existingRoles) {
        final Set<String> combined = new LinkedHashSet<>(existingRoles);
        combined.addAll(incomingRoles);
        assertNoPairConflict(combined);
    }

    // -----------------------------------------------------------------------
    // Dynamic SoD — session-activation enforcement
    // -----------------------------------------------------------------------

    /**
     * Assert that the set of roles being ACTIVATED in a single JWT session does not
     * violate any dynamic SoD constraint. Dynamic constraints allow co-assignment but
     * forbid simultaneous activation (NIST RBAC3 dynamic SoD model).
     *
     * @param activeRoles the set of role names the user wants to activate
     * @throws SodViolationException if any dynamic SoD constraint is violated
     */
    public void assertNoDynamicConflict(Collection<String> activeRoles) {
        final List<SodConstraint> constraints = repository.findAllByDynamicTrue();
        checkConflicts(activeRoles, constraints);
    }

    // -----------------------------------------------------------------------
    // Constraint registration / management
    // -----------------------------------------------------------------------

    /** Register a new SoD constraint. Idempotent if the pair already exists. */
    @Transactional
    public SodConstraint registerConstraint(String roleA, String roleB, String reason, boolean dynamic) {
        if (repository.existsForPair(roleA, roleB)) {
            log.debug("SoD constraint already exists for pair: {} / {}", roleA, roleB);
            return repository.findAll().stream()
                    .filter(c -> c.conflicts(roleA, roleB))
                    .findFirst()
                    .orElseThrow(NotFoundException::new);
        }
        SodConstraint saved = repository.save(SodConstraint.builder()
                .roleNameA(roleA.toUpperCase())
                .roleNameB(roleB.toUpperCase())
                .reason(reason)
                .dynamic(dynamic)
                .build());
        log.info("Registered {} SoD constraint: {} ⊕ {} — {}",
                dynamic ? "dynamic" : "static", roleA, roleB, reason);
        return saved;
    }

    /** Backward-compatible overload used by SodConstraintLoader (always static). */
    @Transactional
    public void registerConstraint(String roleA, String roleB, String reason) {
        registerConstraint(roleA, roleB, reason, false);
    }

    @Transactional
    public void deleteConstraint(Long id) {
        SodConstraint constraint = repository.findById(id).orElseThrow(NotFoundException::new);
        repository.delete(constraint);
        log.info("Deleted SoD constraint id={} ({} ⊕ {})", id,
                constraint.getRoleNameA(), constraint.getRoleNameB());
    }

    /** List all constraints (static + dynamic). */
    public List<SodConstraint> findAll() {
        return repository.findAll();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void checkConflicts(Collection<String> roles, List<SodConstraint> constraints) {
        if (constraints.isEmpty()) {
            return;
        }
        for (final SodConstraint constraint : constraints) {
            final boolean holdsA = roles.stream()
                    .anyMatch(r -> r.equalsIgnoreCase(constraint.getRoleNameA()));
            final boolean holdsB = roles.stream()
                    .anyMatch(r -> r.equalsIgnoreCase(constraint.getRoleNameB()));
            if (holdsA && holdsB) {
                log.warn("SoD violation: roles '{}' and '{}' conflict ({})",
                        constraint.getRoleNameA(), constraint.getRoleNameB(), constraint.getReason());
                throw new SodViolationException(String.format(
                        "SoD violation: roles '%s' and '%s' are mutually exclusive. Reason: %s",
                        constraint.getRoleNameA(), constraint.getRoleNameB(), constraint.getReason()));
            }
        }
    }
}
