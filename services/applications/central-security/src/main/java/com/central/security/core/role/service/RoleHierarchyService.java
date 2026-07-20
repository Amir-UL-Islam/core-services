package com.central.security.core.role.service;

import com.central.security.config.CacheConfig;
import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.repository.RoleRepository;
import com.central.security.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Manages the data-driven RBAC role hierarchy stored in the {@code parent_role_id} column.
 *
 * <p>Industry-standard RBAC (NIST SP 800-192 §3.2) defines role hierarchy as a partial order
 * where a senior role inherits all permissions of its juniors. This service:
 * <ol>
 *   <li>Persists the hierarchy in the DB (self-referential parent FK on {@link Role})</li>
 *   <li>Computes <em>effective privileges</em> for a role by walking the ancestor chain</li>
 *   <li>Builds the Spring Security {@link RoleHierarchyImpl} expression string from DB
 *       so that {@code @PreAuthorize} / method-security respect the DB hierarchy</li>
 * </ol>
 *
 * <p>The {@link com.central.security.core.users.model.entity.Users#getAuthorities()} method
 * calls {@link #collectEffectivePrivileges(Role, Set)} so that hierarchy-inherited privileges
 * are embedded in the JWT's {@code authorities} claim at token generation time.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RoleHierarchyService {

    private final RoleRepository roleRepository;

    // -----------------------------------------------------------------------
    // Parent assignment
    // -----------------------------------------------------------------------

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ROLE_HIERARCHY, allEntries = true)
    public void setParent(Long roleId, Long parentRoleId) {
        Role role = roleRepository.findById(roleId).orElseThrow(NotFoundException::new);
        if (parentRoleId == null) {
            role.setParent(null);
            log.info("Role {} ({}) cleared from hierarchy (now root)", roleId, role.getName());
        } else {
            Role parent = roleRepository.findById(parentRoleId).orElseThrow(
                    () -> new NotFoundException("parent role not found"));
            detectCycle(role, parent);
            role.setParent(parent);
            log.info("Role {} ({}) now inherits from {} ({})",
                    roleId, role.getName(), parentRoleId, parent.getName());
        }
        roleRepository.save(role);
    }

    // -----------------------------------------------------------------------
    // Effective privilege resolution (used by Users.getAuthorities())
    // -----------------------------------------------------------------------

    /**
     * Collect all privileges reachable from {@code role} by walking its ancestor chain.
     * Visited set prevents infinite loops on misconfigured cycles.
     */
    public void collectEffectivePrivileges(Role role, Set<Privilege> accumulator) {
        collectEffectivePrivileges(role, accumulator, new HashSet<>());
    }

    private void collectEffectivePrivileges(Role role, Set<Privilege> accumulator, Set<Long> visited) {
        if (role == null || !visited.add(role.getId())) {
            return; // null-safe + cycle guard
        }
        accumulator.addAll(role.getPrivilege());
        if (role.getParent() != null) {
            collectEffectivePrivileges(role.getParent(), accumulator, visited);
        }
    }

    // -----------------------------------------------------------------------
    // Spring Security RoleHierarchyImpl builder (cached, evicted on changes)
    // -----------------------------------------------------------------------

    /**
     * Build a Spring Security {@link RoleHierarchy} from the current DB state.
     * The result is Caffeine-cached so it is rebuilt only when the hierarchy changes.
     *
     * <p>Expression format: {@code ROLE_SUPER_ADMIN > ROLE_ADMIN \n ROLE_ADMIN > ROLE_USER}
     */
    @Cacheable(value = CacheConfig.CACHE_ROLE_HIERARCHY, key = "'springHierarchy'")
    public RoleHierarchy buildSpringHierarchy() {
        List<Role> roles = roleRepository.findAll();
        StringBuilder sb = new StringBuilder();
        for (Role role : roles) {
            if (role.getParent() != null) {
                // In this model: role.parent = the JUNIOR role this role DOMINATES / inherits FROM.
                // NIST RBAC: a senior role (this role) implies a junior role (parent field).
                // Spring Security: "ROLE_A > ROLE_B" means A implies B (A is senior to B).
                // So we generate: ROLE_{role} > ROLE_{role.parent}
                // Example: SUPER_ADMIN.parent=ADMIN → "ROLE_SUPER_ADMIN > ROLE_ADMIN" ✓
                sb.append("ROLE_").append(role.getName().toUpperCase())
                  .append(" > ROLE_").append(role.getParent().getName().toUpperCase())
                  .append("\n");
            }
        }
        String hierarchyExpr = sb.toString().trim();
        if (hierarchyExpr.isEmpty()) {
            return RoleHierarchyImpl.withDefaultRolePrefix().build();
        }
        log.debug("Role hierarchy expression:\n{}", hierarchyExpr);
        return RoleHierarchyImpl.fromHierarchy(hierarchyExpr);
    }

    // -----------------------------------------------------------------------
    // Hierarchy tree for API / UI
    // -----------------------------------------------------------------------

    public List<Map<String, Object>> getHierarchyTree() {
        List<Role> allRoles = roleRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role role : allRoles) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", role.getId());
            node.put("name", role.getName());
            node.put("description", role.getDescription());
            // "inheritsFromId/Name" = the junior role this role dominates (privilege source)
            node.put("inheritsFromId", role.getParent() != null ? role.getParent().getId() : null);
            node.put("inheritsFromName", role.getParent() != null ? role.getParent().getName() : null);
            result.add(node);
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Cycle detection
    // -----------------------------------------------------------------------

    private void detectCycle(Role role, Role proposedParent) {
        Role cursor = proposedParent;
        Set<Long> visited = new HashSet<>();
        while (cursor != null) {
            if (!visited.add(cursor.getId())) {
                break; // cycle in existing data — tolerate
            }
            if (cursor.getId().equals(role.getId())) {
                throw new IllegalArgumentException(
                        "Setting parent of role '" + role.getName() + "' to '" +
                        proposedParent.getName() + "' would create a hierarchy cycle");
            }
            cursor = cursor.getParent();
        }
    }
}
