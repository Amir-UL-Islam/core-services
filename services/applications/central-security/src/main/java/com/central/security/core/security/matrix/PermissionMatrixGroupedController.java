package com.central.security.core.security.matrix;

import com.problemfighter.pfspring.restapi.rr.Utility;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Grouped privilege matrix API for matrix UIs.
 * <p>
 * Groups privileges by domain and CRUD action using permission-code conventions:
 * resource:action and resource:action:scope.
 * <p>
 * Includes:
 * - GET grouped catalogue by domain and CRUD action
 * - PATCH role updates by domain and CRUD action
 * Examples:
 * - hospital:read -> domain=hospital, action=READ
 * - hospital:read:own -> domain=hospital, action=READ
 * - role:dependency:read -> domain=role:dependency, action=READ
 */
@RestController
@RequestMapping(value = "/api/v1/security/permission-matrix/grouped", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Permission Matrix Grouped", description = "Privileges grouped by domain and CRUD action")
public class PermissionMatrixGroupedController {

    private static final List<String> CRUD_ORDER = List.of("CREATE", "READ", "UPDATE", "DELETE");
    private static final Set<String> CRUD_KEYS = Set.of("CREATE", "READ", "UPDATE", "DELETE");
    private static final Map<String, String> ACTION_ALIASES = Map.ofEntries(
            Map.entry("MANAGE", "UPDATE"),
            Map.entry("ASSIGN", "UPDATE"),
            Map.entry("SWITCH", "UPDATE"),
            Map.entry("VERIFY", "UPDATE"),
            Map.entry("CHALLENGE", "UPDATE"),
            Map.entry("REGISTER", "CREATE")
    );

    private final PrivilegeRepository privilegeRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get privilege catalogue grouped by domain and CRUD")
    public DetailsResponse<GroupedPrivilegeResponse> getGroupedPrivileges() {
        final List<Privilege> privileges = privilegeRepository.findAll(Sort.by("name"));

        final GroupedPrivilegeResponse response = new GroupedPrivilegeResponse();
        response.setPrivilegesByDomainAndCrud(new LinkedHashMap<>());
        response.setUncategorizedPrivileges(new ArrayList<>());

        for (final Privilege privilege : privileges) {
            final PrivilegePlacement placement = resolvePlacement(privilege.getName());
            final PrivilegeEntry entry = toEntry(privilege);

            if (placement == null) {
                response.getUncategorizedPrivileges().add(entry);
                continue;
            }

            final Map<String, List<PrivilegeEntry>> actionMap = response
                    .getPrivilegesByDomainAndCrud()
                    .computeIfAbsent(placement.domain(), ignored -> emptyCrudBuckets());
            actionMap.get(placement.action()).add(entry);
        }

        return Utility.response(response);
    }

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','NICU_ADMIN')")
    @Transactional
    @Operation(summary = "Batch-update role privilege assignments by domain and CRUD")
    public MessageResponse updateGroupedMatrix(@RequestBody @Valid final List<GroupedRoleUpdate> updates) {
        final Map<String, Map<String, Set<Privilege>>> groupedPrivileges = buildGroupedPrivilegeLookup(
                privilegeRepository.findAll(Sort.by("name"))
        );

        for (final GroupedRoleUpdate update : updates) {
            final Role role = roleRepository.findById(update.getRoleId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Role not found: " + update.getRoleId()));

            final Set<Privilege> rolePrivileges = new HashSet<>(role.getPrivilege());
            final Map<String, DomainCrudSelection> domains = update.getDomains();
            if (domains == null || domains.isEmpty()) {
                continue;
            }

            for (final Map.Entry<String, DomainCrudSelection> domainEntry : domains.entrySet()) {
                final String domainKey = normalizeDomain(domainEntry.getKey());
                if (domainKey == null) {
                    continue;
                }
                final Map<String, Set<Privilege>> domainBuckets = groupedPrivileges.get(domainKey);
                if (domainBuckets == null) {
                    continue;
                }
                applyDomainSelection(rolePrivileges, domainBuckets, domainEntry.getValue());
            }

            role.setPrivilege(rolePrivileges);
            roleRepository.save(role);
        }

        return Utility.response("Grouped permission matrix updated successfully");
    }

    private static PrivilegeEntry toEntry(final Privilege privilege) {
        final PrivilegeEntry entry = new PrivilegeEntry();
        entry.setId(privilege.getId());
        entry.setName(privilege.getName());
        return entry;
    }

    private static Map<String, List<PrivilegeEntry>> emptyCrudBuckets() {
        final Map<String, List<PrivilegeEntry>> buckets = new LinkedHashMap<>();
        for (final String key : CRUD_ORDER) {
            buckets.put(key, new ArrayList<>());
        }
        return buckets;
    }

    private static PrivilegePlacement resolvePlacement(final String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return null;
        }

        final String[] rawParts = permissionName.trim().toLowerCase(Locale.ROOT).split(":");
        if (rawParts.length < 2) {
            return null;
        }

        final String[] parts = Arrays.stream(rawParts)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toArray(String[]::new);

        if (parts.length < 2) {
            return null;
        }

        int actionIndex = -1;
        String action = null;
        for (int i = parts.length - 1; i >= 0; i--) {
            final String candidate = parts[i].toUpperCase(Locale.ROOT);
            if (CRUD_KEYS.contains(candidate)) {
                actionIndex = i;
                action = candidate;
                break;
            }
            if (ACTION_ALIASES.containsKey(candidate)) {
                actionIndex = i;
                action = ACTION_ALIASES.get(candidate);
                break;
            }
        }

        if (actionIndex <= 0) {
            return null;
        }

        final String domain = String.join(":", Arrays.copyOfRange(parts, 0, actionIndex));
        if (domain.isBlank()) {
            return null;
        }

        return new PrivilegePlacement(domain, action);
    }

    private static String normalizeDomain(final String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        return domain.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, Map<String, Set<Privilege>>> buildGroupedPrivilegeLookup(final List<Privilege> privileges) {
        final Map<String, Map<String, Set<Privilege>>> grouped = new LinkedHashMap<>();
        for (final Privilege privilege : privileges) {
            final PrivilegePlacement placement = resolvePlacement(privilege.getName());
            if (placement == null) {
                continue;
            }
            grouped
                    .computeIfAbsent(placement.domain(), ignored -> emptyCrudPrivilegeBuckets())
                    .get(placement.action())
                    .add(privilege);
        }
        return grouped;
    }

    private static Map<String, Set<Privilege>> emptyCrudPrivilegeBuckets() {
        final Map<String, Set<Privilege>> buckets = new LinkedHashMap<>();
        for (final String key : CRUD_ORDER) {
            buckets.put(key, new HashSet<>());
        }
        return buckets;
    }

    private static void applyDomainSelection(final Set<Privilege> rolePrivileges,
                                             final Map<String, Set<Privilege>> domainBuckets,
                                             final DomainCrudSelection selection) {
        if (selection == null) {
            return;
        }
        applyAction(rolePrivileges, domainBuckets, "CREATE", selection.getCreate());
        applyAction(rolePrivileges, domainBuckets, "READ", selection.getRead());
        applyAction(rolePrivileges, domainBuckets, "UPDATE", selection.getUpdate());
        applyAction(rolePrivileges, domainBuckets, "DELETE", selection.getDelete());
    }

    private static void applyAction(final Set<Privilege> rolePrivileges,
                                    final Map<String, Set<Privilege>> domainBuckets,
                                    final String action,
                                    final Boolean enabled) {
        if (enabled == null) {
            return;
        }
        final Set<Privilege> actionPrivileges = domainBuckets.get(action);
        if (actionPrivileges == null || actionPrivileges.isEmpty()) {
            return;
        }
        if (enabled) {
            rolePrivileges.addAll(actionPrivileges);
            return;
        }
        rolePrivileges.removeAll(actionPrivileges);
    }

    private record PrivilegePlacement(String domain, String action) {
    }

    @Getter
    @Setter
    public static class GroupedPrivilegeResponse {
        private Map<String, Map<String, List<PrivilegeEntry>>> privilegesByDomainAndCrud;
        private List<PrivilegeEntry> uncategorizedPrivileges;
    }

    @Getter
    @Setter
    public static class GroupedRoleUpdate {
        @NotNull
        private Long roleId;
        @NotNull
        private Map<String, DomainCrudSelection> domains;
    }

    @Getter
    @Setter
    public static class DomainCrudSelection {
        private Boolean create;
        private Boolean read;
        private Boolean update;
        private Boolean delete;
    }

    @Getter
    @Setter
    public static class PrivilegeEntry {
        private Long id;
        private String name;
    }
}

