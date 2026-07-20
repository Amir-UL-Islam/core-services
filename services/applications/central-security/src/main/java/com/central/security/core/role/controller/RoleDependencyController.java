package com.central.security.core.role.controller;

import com.central.security.core.role.model.entity.RoleDependency;
import com.central.security.core.role.service.RoleDependencyService;
import com.central.security.core.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages role prerequisite (dependency) rules.
 *
 * <p>A dependency «A requires B» ensures that whenever role A is assigned to
 * a user, role B must also be present — and vice-versa on removal.
 */
@RestController
@RequestMapping(value = "/api/v1/role-dependencies", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Role Dependencies", description = "APIs for managing role prerequisite rules")
@RequiredArgsConstructor
public class RoleDependencyController {

    private final RoleDependencyService roleDependencyService;

    @GetMapping
    @AdminOnly(reason = "Only admins can view role dependencies")
    @Operation(summary = "List all role dependency rules")
    public ResponseEntity<List<Map<String, Object>>> findAll() {
        final List<Map<String, Object>> result = roleDependencyService.findAll().stream()
                .map(dep -> Map.<String, Object>of(
                        "id", dep.getId(),
                        "dependentRoleId", dep.getDependentRole().getId(),
                        "dependentRoleName", dep.getDependentRole().getName(),
                        "requiredRoleId", dep.getRequiredRole().getId(),
                        "requiredRoleName", dep.getRequiredRole().getName(),
                        "reason", dep.getReason() != null ? dep.getReason() : ""
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @AdminOnly(reason = "Only admins can create role dependency rules")
    @Operation(summary = "Register a new role dependency rule")
    public ResponseEntity<Map<String, Object>> create(@RequestBody @Valid final CreateRequest body) {
        final RoleDependency dep = roleDependencyService.register(
                body.getDependentRoleId(), body.getRequiredRoleId(), body.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", dep.getId(),
                "dependentRoleName", dep.getDependentRole().getName(),
                "requiredRoleName", dep.getRequiredRole().getName()
        ));
    }

    @DeleteMapping("/{id}")
    @AdminOnly(reason = "Only admins can remove role dependency rules")
    @Operation(summary = "Remove a role dependency rule by ID")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        roleDependencyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Request body
    // -----------------------------------------------------------------------

    @Getter
    @Setter
    public static class CreateRequest {

        @NotNull
        private Long dependentRoleId;

        @NotNull
        private Long requiredRoleId;

        @Size(max = 500)
        private String reason;
    }
}
