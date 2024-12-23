package com.central.security.model.dtos;

import com.central.security.model.enums.Permissions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrivilegeDTO {
    private Long created;
    private String name;
    private String label;
    private String urlPattern;
    private String httpMethod;
    private Permissions permissions;
    private String description;
}
