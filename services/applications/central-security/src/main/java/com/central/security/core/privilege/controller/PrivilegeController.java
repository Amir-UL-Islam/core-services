package com.central.security.core.privilege.controller;

import com.problemfighter.pfspring.restapi.inter.RestApiAction;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.central.security.core.privilege.model.dto.PrivilegeDTO;
import com.central.security.core.privilege.service.implmentation.PrivilegeService;
import com.central.security.core.privilege.service.implmentation.PrivilegeServiceCacheable;
import com.central.security.core.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/privileges", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Privilege", description = "APIs for managing user privileges and their associated URLs")
public class PrivilegeController implements RestApiAction<PrivilegeDTO, PrivilegeDTO, PrivilegeDTO> {

    private final PrivilegeService privilegeService;
    private final PrivilegeServiceCacheable cacheableService;


    @Operation(
            parameters = {
                    @Parameter(
                            name = "page",
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = Integer.class)
                    ),
                    @Parameter(
                            name = "size",
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = Integer.class)
                    ),
                    @Parameter(
                            name = "sort",
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = String.class)
                    )
            }
    )
    @GetMapping
    public PageableResponse<PrivilegeDTO> getAllPrivileges(
            @Parameter(hidden = true) @SortDefault(sort = "id") @PageableDefault(size = 20) final Pageable pageable
    ) {
        return privilegeService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public DetailsResponse<PrivilegeDTO> getPrivilege(@PathVariable final Long id) {
        return privilegeService.getDetails(id);
    }

    @PostMapping
    @AdminOnly(reason = "Only admins can create new privileges")
    @ApiResponse(responseCode = "201")
    public MessageResponse create(@RequestBody @Valid final RequestData<PrivilegeDTO> privilegeDTO) {
        return cacheableService.create(privilegeDTO);
    }

    @PatchMapping
    @AdminOnly(reason = "Only admins can modify privileges")
    public MessageResponse update(@RequestBody @Valid final RequestData<PrivilegeDTO> privilegeDTO) {
        return cacheableService.update(privilegeDTO);
    }

    @PatchMapping("/assign/url")
    @AdminOnly(reason = "Only admins can assign URLs to privileges")
    @ApiResponse(responseCode = "201")
    public MessageResponse assignUrl(@RequestParam(name = "privilegeId") final Long privilegeId,
                                     @RequestParam(name = "urlId") final Long urlId) {
        return cacheableService.assignUrl(privilegeId, urlId);
    }

    @PatchMapping({"/remove/assess/url", "/remove/assign/url"})
    @AdminOnly(reason = "Only admins can remove URL assignments from privileges")
    @ApiResponse(responseCode = "201")
    public MessageResponse removeUrl(@RequestParam(name = "privilegeId") final Long privilegeId,
                                     @RequestParam(name = "urlId") final Long urlId) {
        return cacheableService.removeAssignUrl(privilegeId, urlId);
    }

    @DeleteMapping("/{id}")
    @AdminOnly(reason = "Only admins can delete privileges")
    @ApiResponse(responseCode = "204")
    public MessageResponse deletePrivilege(@PathVariable final Long id) {
        return cacheableService.delete(id);
    }

}
