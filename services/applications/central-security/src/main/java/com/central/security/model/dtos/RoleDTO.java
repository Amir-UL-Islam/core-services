package com.central.security.model.dtos;

import com.central.security.model.enums.Roles;
import lombok.Data;

import java.util.List;

@Data
public class RoleDTO {
    private Long created;

    private Roles name;

    private String description;

    private List<Long> privilegeIds;

    private boolean restricted = true;
}
