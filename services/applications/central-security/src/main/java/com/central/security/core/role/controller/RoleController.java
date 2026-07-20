package com.central.security.core.role.controller;

import com.problemfighter.pfspring.restapi.inter.RestApiAction;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.central.security.core.privilege.service.implmentation.PrivilegeService;
import com.central.security.core.role.model.dto.RoleDTO;
import com.central.security.core.role.service.RoleHierarchyService;
import com.central.security.core.role.service.RoleService;
import com.central.security.core.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Role", description = "APIs for managing user roles and their associated privileges")
@RequiredArgsConstructor
public class RoleController implements RestApiAction<RoleDTO, RoleDTO, RoleDTO> {

    private final RoleService roleService;
    private final PrivilegeService privilegeService;
    private final RoleHierarchyService roleHierarchyService;


    @GetMapping
    public PageableResponse<RoleDTO> getAllRoles(
            @RequestParam(name = "query", required = false) final String query,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "direction", required = false, defaultValue = "ASC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, direction, "id");
        return roleService.findAll(query, pageable);
    }

    @GetMapping("/{id}")
    public DetailsResponse<RoleDTO> getRole(@PathVariable final Long id) {
        return roleService.getDetails(id);
    }

    @PostMapping
    @AdminOnly(reason = "Only admins can create new roles")
    @ApiResponse(responseCode = "201")
    public MessageResponse createRole(@RequestBody @Valid final RequestData<RoleDTO> roleDTO) {
        return roleService.create(roleDTO);
    }

    @PutMapping("/{id}")
    @AdminOnly(reason = "Only admins can modify roles")
    public MessageResponse updateRole(@PathVariable final Long id,
                                      @RequestBody @Valid final RequestData<RoleDTO> roleDTO) {
        roleDTO.getData().setId(id);
        return roleService.update(roleDTO);
    }

    @DeleteMapping("/{id}")
    @AdminOnly(reason = "Only admins can delete roles")
    @ApiResponse(responseCode = "204")
    public MessageResponse deleteRole(@PathVariable(name = "id") final Long id) {
        return roleService.delete(id);
    }

    @GetMapping("/privilegeValues")
    public Map<Long, String> getPrivilegeValues() {
        return privilegeService.getPrivilegeValues();
    }

    @GetMapping("/hierarchy")
    @Operation(summary = "Get full role hierarchy tree")
    public List<Map<String, Object>> getHierarchy() {
        return roleHierarchyService.getHierarchyTree();
    }

    @PutMapping("/{id}/parent")
    @AdminOnly(reason = "Only admins can manage role hierarchy")
    @Operation(summary = "Set or clear the parent of a role (hierarchy management)")
    public Map<String, String> setParent(
            @PathVariable final Long id,
            @RequestBody final Map<String, Object> body) {
        Object parentIdRaw = body.get("parentId");
        Long parentId = parentIdRaw != null ? Long.valueOf(parentIdRaw.toString()) : null;
        roleHierarchyService.setParent(id, parentId);
        return Map.of("message", "Role hierarchy updated");
    }

}
