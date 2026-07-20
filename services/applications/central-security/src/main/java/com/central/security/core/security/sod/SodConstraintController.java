package com.central.security.core.security.sod;

import com.central.security.core.security.sod.dto.SodConstraintDTO;
import com.central.security.core.security.sod.model.entity.SodConstraint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing Separation-of-Duties (SoD) constraints.
 *
 * Requires {@code sod:read} to list and {@code sod:manage} to create/delete.
 * Both permissions are granted to SUPER_ADMIN via RoleLoader.
 */
@RestController
@RequestMapping(value = "/api/v1/security/sod", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "SoD Constraints", description = "Manage Separation-of-Duties constraints between roles")
public class SodConstraintController {

    private final SodConstraintService sodConstraintService;

    @GetMapping
    @Operation(summary = "List all SoD constraints (static + dynamic)")
    public ResponseEntity<List<SodConstraintDTO>> listAll() {
        List<SodConstraintDTO> result = sodConstraintService.findAll().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Create a new SoD constraint")
    public ResponseEntity<SodConstraintDTO> create(@RequestBody @Valid SodConstraintDTO dto) {
        SodConstraint saved = sodConstraintService.registerConstraint(
                dto.getRoleNameA(), dto.getRoleNameB(), dto.getReason(), dto.isDynamic());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a SoD constraint by id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sodConstraintService.deleteConstraint(id);
        return ResponseEntity.noContent().build();
    }

    private SodConstraintDTO toDTO(SodConstraint c) {
        SodConstraintDTO dto = new SodConstraintDTO();
        dto.setId(c.getId());
        dto.setRoleNameA(c.getRoleNameA());
        dto.setRoleNameB(c.getRoleNameB());
        dto.setReason(c.getReason());
        dto.setDynamic(c.isDynamic());
        return dto;
    }
}
