package com.central.security.core.security.matrix;

import com.problemfighter.pfspring.restapi.rr.Utility;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
import com.central.security.core.role.RoleLoader;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for inspecting and batch-updating the roles × privileges permission matrix.
 * <p>
 * GET /api/v1/security/permission-matrix — Returns all roles with their assigned privilege IDs
 * plus the full privilege catalogue.
 * PUT /api/v1/security/permission-matrix — Replaces privilege assignments for the supplied roles
 * (admin-only).
 */
@RestController
@RequestMapping(value = "/api/v1/security/permission-matrix", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Permission Matrix", description = "Full roles × privileges matrix management")
public class PermissionMatrixController {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final RoleLoader roleLoader;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get the full roles × privileges assignment matrix")
    public DetailsResponse<MatrixResponse> getMatrix() {
        final List<Role> roles = roleRepository.findAll(Sort.by("name"));
        final List<Privilege> privileges = privilegeRepository.findAll(Sort.by("name"));

        final MatrixResponse response = new MatrixResponse();
        response.setRoles(roles.stream().map(r -> {
            final RoleEntry re = new RoleEntry();
            re.setId(r.getId());
            re.setName(r.getName());
            re.setDescription(r.getDescription());
            re.setPrivilegeIds(r.getPrivilege().stream()
                    .map(Privilege::getId)
                    .collect(Collectors.toSet()));
            return re;
        }).toList());
        response.setPrivileges(privileges.stream().map(p -> {
            final PrivilegeEntry pe = new PrivilegeEntry();
            pe.setId(p.getId());
            pe.setName(p.getName());
            return pe;
        }).toList());
        return Utility.response(response);
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','NICU_ADMIN')")
    @Transactional
    @Operation(summary = "Batch-update role privilege assignments")
    public MessageResponse updateMatrix(@RequestBody @Valid final List<RolePrivilegesUpdate> updates) {
        for (final RolePrivilegesUpdate update : updates) {
            final Role role = roleRepository.findById(update.getRoleId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Role not found: " + update.getRoleId()));
            final Set<Privilege> newPrivileges =
                    new HashSet<>(privilegeRepository.findAllById(update.getPrivilegeIds()));
            role.setPrivilege(newPrivileges);
            roleRepository.save(role);
        }
        return Utility.response("Permission matrix updated successfully");
    }

    @PostMapping(value = "/reset")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Operation(summary = "Reset permission matrix to default initialization state (SUPER_ADMIN only)")
    public MessageResponse resetMatrixToDefaults() {
        roleLoader.resetRolePrivilegeMatrixToDefaults();
        return Utility.response("Permission matrix reset to default state");
    }

    // ---- Response DTOs -------------------------------------------------------

    @Getter
    @Setter
    public static class MatrixResponse {
        private List<RoleEntry> roles;
        private List<PrivilegeEntry> privileges;
    }

    @Getter
    @Setter
    public static class RoleEntry {
        private Long id;
        private String name;
        private String description;
        private Set<Long> privilegeIds;
    }

    @Getter
    @Setter
    public static class PrivilegeEntry {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    public static class RolePrivilegesUpdate {
        @NotNull
        private Long roleId;
        @NotNull
        private Set<Long> privilegeIds;
    }
}
