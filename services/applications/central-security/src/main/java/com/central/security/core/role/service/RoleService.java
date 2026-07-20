package com.central.security.core.role.service;

import com.central.security.config.CacheConfig;
import com.central.security.events.BeforeDeletePrivilege;
import com.central.security.events.BeforeDeleteRole;
import com.central.security.util.CustomCollectors;
import com.central.security.core.role.repository.RoleRepository;
import com.central.security.core.role.model.dto.RoleDTO;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.security.audit.model.entity.PolicyChangeAudit;
import com.central.security.core.security.audit.service.PolicyChangeAuditService;
import com.central.security.core.security.util.TokenVersionManager;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;
import com.central.security.util.NotFoundException;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.problemfighter.pfspring.restapi.rr.RequestResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class RoleService implements RequestResponse {

    private final RoleRepository roleRepository;
    private final UsersRepository usersRepository;
    private final TokenVersionManager tokenVersionManager;
    private final ApplicationEventPublisher publisher;
    private final PolicyChangeAuditService policyChangeAuditService;


    public PageableResponse<RoleDTO> findAll(final String query, final Pageable pageable) {
        Page<Role> page = roleRepository.search(query, pageable);
        return responseProcessor().response(page, RoleDTO.class);
    }

    public DetailsResponse<RoleDTO> getDetails(final Long id) {
        return responseProcessor().response(roleRepository.findById(id), RoleDTO.class);
    }

    /**
     * Backward-compatible DTO accessor used by validation annotations.
     */
    public RoleDTO get(final Long id) {
        return roleRepository.findById(id)
                .map(role -> responseProcessor().entityToDTO(role, RoleDTO.class))
                .orElseThrow(NotFoundException::new);
    }

    public MessageResponse create(final RequestData<RoleDTO> data) {
        final Role role = new Role();
        requestProcessor().process(data, role);
        final Role saved = roleRepository.save(role);
        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.ROLE, saved.getId(),
                PolicyChangeAudit.ChangeType.CREATE,
                currentUserId(), currentUsername(),
                null, saved.getName(),
                "Role created");
        return responseProcessor().response("Role created with ID: " + saved.getId());
    }

    @CacheEvict(value = {CacheConfig.CACHE_PRIVILEGES, CacheConfig.CACHE_ROLE_HIERARCHY}, allEntries = true)
    public MessageResponse update(final RequestData<RoleDTO> data) {
        requestProcessor().validateId(data.getData().getId(), "Role ID can't be null");
        final Long id = data.getData().getId();
        final Role role = roleRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        // Prevent modification of system roles
        if (isSystemRole(role.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cannot edit system role: " + role.getName());
        }

        final String previousName = role.getName();
        requestProcessor().process(data, role);
        roleRepository.save(role);

        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.ROLE, id,
                PolicyChangeAudit.ChangeType.UPDATE,
                currentUserId(), currentUsername(),
                previousName, role.getName(),
                "Role updated");

        // Invalidate tokens for all users with this role so permission changes take effect immediately
        usersRepository.findUsersWithRole(id).forEach(user -> {
            tokenVersionManager.invalidateAccessTokens(user.getId());
            log.info("Invalidated access tokens for user {} due to role {} update", user.getId(), id);
        });
        return responseProcessor().response("Role updated with ID: " + id);
    }

    @CacheEvict(value = {CacheConfig.CACHE_PRIVILEGES, CacheConfig.CACHE_ROLE_HIERARCHY}, allEntries = true)
    public MessageResponse delete(final Long id) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        // Prevent deletion of system roles
        if (isSystemRole(role.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cannot delete system role: " + role.getName());
        }

        // Check if role is assigned to any users
        final List<Users> usersWithRole =
                usersRepository.findUsersWithRole(id);
        if (!usersWithRole.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete role '" + role.getName() + "': It is assigned to " +
                            usersWithRole.size() + " user(s). Please unassign the role from all users before deletion.");
        }

        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.ROLE, id,
                PolicyChangeAudit.ChangeType.DELETE,
                currentUserId(), currentUsername(),
                role.getName(), null,
                "Role deleted");

        // Invalidate tokens for all users with this role before deletion
        usersWithRole.forEach(user -> {
            tokenVersionManager.invalidateAccessTokens(user.getId());
            log.info("Invalidated access tokens for user {} due to role {} deletion", user.getId(), id);
        });

        publisher.publishEvent(new BeforeDeleteRole(id));
        roleRepository.delete(role);
        return responseProcessor().response("Role deleted with ID: " + id);
    }

    public boolean nameExists(final String name) {
        return roleRepository.existsByNameIgnoreCase(name);
    }

    public boolean descriptionExists(final String description) {
        return roleRepository.existsByDescriptionIgnoreCase(description);
    }

    public Map<Long, String> getRoleValues() {
        return roleRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Role::getId, Role::getName));
    }

    @EventListener(BeforeDeletePrivilege.class)
    public void on(final BeforeDeletePrivilege event) {
        // remove many-to-many relations at owning side
        roleRepository.findAllByPrivilegeId(event.getId()).forEach(role ->
                role.getPrivilege().removeIf(privilege -> privilege.getId().equals(event.getId())));
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    public Optional<Role> getSystemRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    private boolean isSystemRole(final String roleName) {
        return "SUPER_ADMIN".equalsIgnoreCase(roleName) ||
                "ADMIN".equalsIgnoreCase(roleName) ||
                "USER".equalsIgnoreCase(roleName) ||
                "NICU_ADMIN".equalsIgnoreCase(roleName) ||
                "HOSPITAL".equalsIgnoreCase(roleName) ||
                "AMBULANCE".equalsIgnoreCase(roleName);
    }

    // -------------------------------------------------------------------------
    // Audit helpers
    // -------------------------------------------------------------------------

    private Long currentUserId() {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Users u) {
                return u.getId();
            }
        } catch (Exception ignored) { /* best-effort */ }
        return null;
    }

    private String currentUsername() {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
                return ud.getUsername();
            }
        } catch (Exception ignored) { /* best-effort */ }
        return "system";
    }
}
