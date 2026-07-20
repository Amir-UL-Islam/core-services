package com.central.security.core.users.controller;

import com.problemfighter.pfspring.restapi.inter.RestApiAction;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.central.security.core.role.model.enums.SystemRoles;
import com.central.security.core.role.service.RoleService;
import com.central.security.core.security.oauth.clientcredentials.annotation.MakeThisAvailableForMachineClient;
import com.central.security.core.users.model.dto.UsersDTO;
import com.central.security.core.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/v1/user", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User", description = "APIs for managing users and their associated roles")
@RequiredArgsConstructor
public class UsersController implements RestApiAction<UsersDTO, UsersDTO, UsersDTO> {
    private final UsersService usersService;
    private final RoleService roleService;

    @GetMapping
    @Transactional(readOnly = true)
    @MakeThisAvailableForMachineClient(scopes = {"m2m.read"})
    public PageableResponse<UsersDTO> getAllUsers(
            @RequestParam(name = "query", required = false) final String query,
            @RequestParam(name = "systemRoles", required = false) final SystemRoles systemRoles,
            @RequestParam(name = "withDeletedUser", required = false) final Boolean withDeletedUser,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "direction", required = false, defaultValue = "ASC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, direction, "id");
        return usersService.findAll(query, systemRoles, withDeletedUser, pageable);
    }

    @GetMapping("/{id}")
    public DetailsResponse<UsersDTO> getUsers(@PathVariable final Long id) {
        return usersService.get(id);
    }

    @PostMapping
    @Operation(hidden = true)
    @ApiResponse(responseCode = "201")
    public MessageResponse create(@RequestBody @Valid final RequestData<UsersDTO> data) {
        return usersService.create(data);
    }

    @PatchMapping
    public MessageResponse update(@RequestBody @Valid final RequestData<UsersDTO> data) {
        return usersService.update(data);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public MessageResponse delete(@PathVariable final Long id) {
        return usersService.delete(id);
    }

    @GetMapping("/roleValues")
    public ResponseEntity<Map<Long, String>> getRoleValues() {
        return ResponseEntity.ok(roleService.getRoleValues());
    }
}
