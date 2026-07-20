package com.central.security.core.urls.model.dto;

import com.problemfighter.java.oc.annotation.DataMappingInfo;
import com.problemfighter.pfspring.restapi.inter.model.RestDTO;
import com.problemfighter.java.base.dto.BaseUserDTO;
import com.central.security.core.urls.model.mapper.UrlInterceptor;
import com.central.security.core.urls.UrlsEndpointUnique;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


/**
 * DTO for creating/updating {@code Url} endpoint definitions.
 *
 * The composite {@code @UrlsEndpointUnique} constraint ensures the
 * (endpoint, method) pair is unique — matching the {@code uk_endpoint_method}
 * database constraint — without rejecting legitimate same-path/different-method combinations.
 */
@DataMappingInfo(customProcessor = UrlInterceptor.class)
@Getter
@Setter
@UrlsEndpointUnique
public class UrlDTO extends BaseUserDTO implements RestDTO {

    @NotNull
    @Size(max = 255)
    private String endpoint;

    @NotNull
    @Size(max = 255)
    private String method;

    @NotNull
    private List<Long> privileges;

}
