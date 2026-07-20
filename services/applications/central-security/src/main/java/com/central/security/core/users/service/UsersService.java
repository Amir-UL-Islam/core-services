package com.central.security.core.users.service;

import com.central.security.events.BeforeDeleteRole;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.model.enums.SystemRoles;
import com.central.security.core.security.audit.model.entity.PolicyChangeAudit;
import com.central.security.core.security.audit.service.PolicyChangeAuditService;
import com.central.security.core.security.oauth.SecurityContext;
import com.central.security.core.security.util.TokenVersionManager;
import com.central.security.core.users.model.dto.UsersDTO;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;
import com.central.security.util.NotFoundException;
import com.problemfighter.pfspring.restapi.rr.RequestResponse;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;


@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class UsersService implements RequestResponse {

    private final UsersRepository usersRepository;
    private final TokenVersionManager tokenVersionManager;
    private final PolicyChangeAuditService policyChangeAuditService;
    private final PasswordEncoder passwordEncoder;

    public PageableResponse<UsersDTO> findAll(final String query, SystemRoles systemRoles, Boolean withDeletedUser, final Pageable pageable) {
        Page<Users> page;
        String role = systemRoles != null ? systemRoles.name() : null;
        if (withDeletedUser != null && withDeletedUser) {
            page = usersRepository.searchWithDeletedUsers(query, role, pageable);
        } else {
            page = usersRepository.searchUsers(query, role, pageable);
        }
        return responseProcessor().response(page, UsersDTO.class);
    }

    public DetailsResponse<UsersDTO> get(final Long id) {
        return responseProcessor().response(usersRepository.findById(id), UsersDTO.class);
    }

    public MessageResponse create(final RequestData<UsersDTO> data) {
        final Users users = new Users();
        requestProcessor().process(data, users);
        final Users saved = usersRepository.save(users);
        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.USER_ROLE_ASSIGNMENT, saved.getId(),
                PolicyChangeAudit.ChangeType.CREATE,
                currentUserId(), currentUsername(),
                null, saved.getUsername(),
                "User created");
        return responseProcessor().response("User created with ID: " + saved.getId());
    }

    public MessageResponse update(RequestData<UsersDTO> data) {
        if (
                data.getData().getPassword() != null
                        && !data.getData().getPassword().trim().isEmpty()
                        && !SecurityContext.isSuperAdmin()
        ) {
            throw new ResponseStatusException(BAD_REQUEST, "For updating password, provide Current Password and new Password");
        }
        final Users users = dataUtil().validateAndOptionToEntity(usersRepository.findById(data.getData().getId()), "User Not Found");

        // Password update: newPassword triggers a password change.
        // currentPassword must be verified against the stored hash when changing an existing password.
        final String newPassword = data.getData().getNewPassword();
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (users.getPassword() != null && !users.getPassword().trim().isEmpty()) {
                final String currentPassword = data.getData().getCurrentPassword();
                if ((currentPassword == null || currentPassword.trim().isEmpty()) && !SecurityContext.isSuperAdmin()) {
                    throw new ResponseStatusException(BAD_REQUEST, "Current password is required to change the password");
                }
                if (!passwordEncoder.matches(currentPassword, users.getPassword()) && !SecurityContext.isSuperAdmin()) {
                    throw new ResponseStatusException(BAD_REQUEST, "Current password is incorrect");
                }
            }
        }

        final String previousRoles = users.getRole().stream()
                .map(Role::getName).reduce("", (a, b) -> a + "," + b);

        requestProcessor().process(data, users);
        usersRepository.save(users);

        policyChangeAuditService.recordChange(
                PolicyChangeAudit.PolicyType.USER_ROLE_ASSIGNMENT, data.getData().getId(),
                PolicyChangeAudit.ChangeType.UPDATE,
                currentUserId(), currentUsername(),
                previousRoles.isEmpty() ? null : previousRoles,
                users.getRole().stream().map(Role::getName).reduce("", (a, b) -> a + "," + b),
                "User roles updated — tokens invalidated");

        // Role set may have changed — invalidate tokens so new privileges take effect immediately
        tokenVersionManager.invalidateAccessTokens(data.getData().getId());
        log.info("Invalidated access tokens for user {} after profile/role update", data.getData().getId());
        return responseProcessor().response("User updated successfully");
    }

    public MessageResponse delete(final Long id) {
        final Users users = usersRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        // Invalidate all tokens before deletion to prevent lingering sessions
        tokenVersionManager.invalidateAllTokens(id);
        users.setDeleted(true);
        usersRepository.save(users);
        return responseProcessor().response("User deleted successfully");
    }

    public boolean usernameExists(final String username) {
        return usersRepository.existsByUsernameIgnoreCase(username);
    }

    @EventListener(BeforeDeleteRole.class)
    public void on(final BeforeDeleteRole event) {
        // remove many-to-many relations at owning side
        usersRepository.findAllByRoleId(event.getId()).forEach(users ->
                users.getRole().removeIf(role -> role.getId().equals(event.getId())));
    }

    public Users findByUsername(String name) {
        return usersRepository.findByUsernameIgnoreCase(name);
    }

    /**
     * Revoke ALL sessions for the current user by incrementing both token versions.
     * Uses {@link TokenVersionManager} for a consistent, deterministic increment (never random).
     */
    public void logoutAllDevices(String username) {
        final Users user = usersRepository.findByUsernameIgnoreCase(username);
        if (user == null) {
            return;
        }
        tokenVersionManager.invalidateAllTokens(user.getId());
        log.info("All sessions revoked for user {}", username);
    }

    public Optional<Users> findById(Long ownerId) {
        return usersRepository.findById(ownerId);
    }

    // -------------------------------------------------------------------------
    // Audit helpers
    // -------------------------------------------------------------------------

    private Long currentUserId() {
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.central.security.core.users.model.entity.Users u) {
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

    public Boolean existsById(Long patientId) {
        return usersRepository.existsById(patientId);
    }

    public void save(Users owner) {
        usersRepository.save(owner);
    }

    public Users findByUsernameIgnoreCase(String name) {
        return usersRepository.findByUsernameIgnoreCase(name);
    }
}
