package com.central.security.core.role.model.dto;

import com.central.security.core.role.RoleDescriptionUnique;
import com.central.security.core.role.RoleNameUnique;
import com.central.security.core.role.model.mapper.RoleInterceptor;
import com.problemfighter.java.base.dto.BaseUserDTO;
import com.problemfighter.java.oc.annotation.DataMappingInfo;
import com.problemfighter.pfspring.restapi.inter.model.RestDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


@DataMappingInfo(customProcessor = RoleInterceptor.class)
@Getter
@Setter
public class RoleDTO extends BaseUserDTO implements RestDTO {


    @NotNull
    @Size(max = 255)
    @RoleNameUnique
    private String name;

    @NotNull
    @Size(max = 255)
    @RoleDescriptionUnique
    private String description;

    private List<Long> privilege;

    /** ID of the parent role in the hierarchy. Null = root role. */
    private Long parentId;

}
