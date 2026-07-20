package com.central.security.core.role.service;

import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.model.entity.RoleDependency;
import com.central.security.core.role.repository.RoleDependencyRepository;
import com.central.security.core.role.repository.RoleRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.central.security.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Enforces DB-driven role prerequisite rules.
 *
 * <p>A dependency «A requires B» means:
 * <ul>
 *   <li>Assigning role A to a user who does not have role B is rejected.</li>
 *   <li>Removing role B from a user who still has role A is rejected.</li>
 *   <li>A role that bundles both A and B together satisfies its own dependency.</li>
 * </ul>
 *
 * Validation is performed against the <em>complete final role set</em> the user
 * would hold after the operation — so both add and remove paths go through the
 * same {@link #assertSatisfied(Set)} method.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RoleDependencyService {

    private final RoleDependencyRepository repository;
    private final RoleRepository roleRepository;

    // -----------------------------------------------------------------------
    // Enforcement
    // -----------------------------------------------------------------------

    /**
     * Assert that every role in {@code finalRoles} has all its required prerequisite
     * roles also present in the same set.
     *
     * @param finalRoles the complete set of roles the user would hold after the operation
     * @throws ResponseStatusException HTTP 422 if any dependency is unsatisfied
     */
    public void assertSatisfied(final Set<Role> finalRoles) {
        if (finalRoles.isEmpty()) {
            return;
        }
        final Set<Long> roleIds = finalRoles.stream().map(Role::getId).collect(Collectors.toSet());
        final List<RoleDependency> deps = repository.findAllByDependentRoleIdIn(roleIds);
        for (final RoleDependency dep : deps) {
            if (!roleIds.contains(dep.getRequiredRole().getId())) {
                log.warn("Role dependency violation: '{}' requires '{}' but it is not present",
                        dep.getDependentRole().getName(), dep.getRequiredRole().getName());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Role '" + dep.getDependentRole().getName()
                        + "' requires role '" + dep.getRequiredRole().getName()
                        + "' to also be assigned."
                        + (dep.getReason() != null ? " Reason: " + dep.getReason() : ""));
            }
        }
    }

    // -----------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------

    @Transactional
    public RoleDependency register(final Long dependentRoleId, final Long requiredRoleId, final String reason) {
        final Role dependent = roleRepository.findById(dependentRoleId)
                .orElseThrow(() -> new NotFoundException("Dependent role not found"));
        final Role required = roleRepository.findById(requiredRoleId)
                .orElseThrow(() -> new NotFoundException("Required role not found"));

        if (dependent.getId().equals(required.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A role cannot depend on itself");
        }
        if (repository.existsByDependentRoleAndRequiredRole(dependent, required)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dependency already exists: '" + dependent.getName()
                    + "' → '" + required.getName() + "'");
        }

        final RoleDependency dep = new RoleDependency();
        dep.setDependentRole(dependent);
        dep.setRequiredRole(required);
        dep.setReason(reason);
        final RoleDependency saved = repository.save(dep);
        log.info("Registered role dependency: '{}' requires '{}'", dependent.getName(), required.getName());
        return saved;
    }

    @Transactional
    public void delete(final Long id) {
        final RoleDependency dep = repository.findById(id).orElseThrow(NotFoundException::new);
        repository.delete(dep);
        log.info("Deleted role dependency id={} ('{}' → '{}')", id,
                dep.getDependentRole().getName(), dep.getRequiredRole().getName());
    }

    public List<RoleDependency> findAll() {
        return repository.findAll();
    }
}
