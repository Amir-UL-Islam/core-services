package com.central.security.model.mappers;

import com.central.security.model.dtos.PrivilegeDTO;
import com.central.security.model.entites.Privilege;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PrivilegeMapper {
    public Privilege map(PrivilegeDTO dto) {
        Privilege entity = new Privilege();
        entity.setCreated(new Date(dto.getCreated() == null || dto.getCreated() == 0 ? System.currentTimeMillis() : dto.getCreated()));
        entity.setName(dto.getName());
        entity.setLabel(dto.getLabel());
        entity.setDescription(dto.getDescription());
        entity.setHttpMethod(dto.getHttpMethod());
        entity.setUrlPattern(dto.getUrlPattern());
        entity.setPermissions(dto.getPermissions());
        return entity;
    }

    public PrivilegeDTO map(Privilege entity) {
        PrivilegeDTO dto = new PrivilegeDTO();
        dto.setHttpMethod(entity.getHttpMethod());
        dto.setUrlPattern(entity.getUrlPattern());
        dto.setPermissions(entity.getPermissions());
        dto.setCreated(entity.getCreated().getTime());
        dto.setDescription(entity.getDescription());
        dto.setLabel(entity.getLabel());
        dto.setName(entity.getName());
        return dto;
    }
}
