package com.central.security.core.urls.controller;

import com.problemfighter.pfspring.restapi.inter.RestApiAction;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.central.security.core.privilege.service.implmentation.PrivilegeService;
import com.central.security.core.urls.model.dto.UrlDTO;
import com.central.security.core.urls.service.UrlService;
import com.central.security.core.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/v1/url", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "URLs", description = "APIs for managing URLs and their associated privileges")
@RequiredArgsConstructor
public class UrlController implements RestApiAction<UrlDTO, UrlDTO, UrlDTO> {

    private final UrlService urlsService;
    private final PrivilegeService privilegeService;


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
    @Transactional(readOnly = true)
    public PageableResponse<UrlDTO> getAllUrls(
            @RequestParam(name = "filter", required = false) final String filter,
            @Parameter(hidden = true) @SortDefault(sort = "id")
            @PageableDefault(size = 20) final Pageable pageable) {
        return urlsService.findAll(filter, pageable);
    }

    @GetMapping("/{id}")
    public DetailsResponse<UrlDTO> getUrls(@PathVariable final Long id) {
        return urlsService.get(id);
    }

    @GetMapping("/privilege/{id}")
    @Transactional(readOnly = true)
    public DetailsResponse<UrlDTO> getByPrivilegeId(@PathVariable final Long id) {
        return urlsService.getByPrivilege(id);
    }

    @PostMapping
    @AdminOnly(reason = "Only admins can create new URL endpoints")
    @ApiResponse(responseCode = "201")
    public MessageResponse create(@RequestBody @Valid final RequestData<UrlDTO> urlsDTO) {
        return urlsService.create(urlsDTO);
    }

    @PutMapping
    @AdminOnly(reason = "Only admins can modify URL endpoints")
    public MessageResponse update(@RequestBody @Valid final RequestData<UrlDTO> urlsDTO) {
        return urlsService.update(urlsDTO);
    }

    @Transactional
    @DeleteMapping("/{id}")
    @AdminOnly(reason = "Only admins can delete URL endpoints")
    @ApiResponse(responseCode = "204")
    public MessageResponse deleteUrls(@PathVariable final Long id) {
        return urlsService.delete(id);
    }

    @GetMapping("/privilegeValues")
    public Map<Long, String> getPrivilegeValues() {
        return privilegeService.getPrivilegeValues();
    }

}
