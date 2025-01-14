package com.central.security.model.mappers;

import com.central.security.model.dtos.RoleDTO;
import com.central.security.model.entites.Role;
import com.central.security.services.PrivilegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleMapper {
    private final PrivilegeService privilegeService;

    public Role map(RoleDTO dto) {
        Role entity = new Role();
        entity.setCreated(new Date(dto.getCreated() == null || dto.getCreated() == 0 ? System.currentTimeMillis() : dto.getCreated()));
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        dto.getPrivilegeIds().forEach(it ->
                entity.getPrivileges().add(privilegeService.findById(it))
        );
        return entity;
    }
}
