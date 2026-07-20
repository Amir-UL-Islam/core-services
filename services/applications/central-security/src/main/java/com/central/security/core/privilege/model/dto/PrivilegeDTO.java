package com.central.security.core.privilege.model.dto;

import com.central.security.core.privilege.PermissionCodeFormat;
import com.central.security.core.privilege.PrivilegeNameUnique;
import com.central.security.core.privilege.model.mapper.PrivilegeInterceptor;
import com.problemfighter.java.base.dto.BaseUserDTO;
import com.problemfighter.java.oc.annotation.DataMappingInfo;
import com.problemfighter.pfspring.restapi.inter.model.RestDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@DataMappingInfo(customProcessor = PrivilegeInterceptor.class)
@Getter
@Setter
public class PrivilegeDTO extends BaseUserDTO implements RestDTO {

    @NotNull
    @Size(max = 255)
    @PrivilegeNameUnique
    @PermissionCodeFormat
    private String name;

}
