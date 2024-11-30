package com.central.security.model.mappers;

import com.central.security.model.dtos.PrivilegeDTO;
import com.central.security.model.entites.Privilege;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PrivilegeMapper {
    public Privilege map(PrivilegeDTO dto) {
        Privilege entity = new Privilege();
        entity.setCreated(new Date(dto.getCreated() == null || dto.getCreated() == 0 ? System.currentTimeMillis() : dto.getCreated()));
        entity.setName(dto.getName());
        entity.setLabel(dto.getLabel());
        entity.setDescription(dto.getDescription());
        entity.setAccessUrls(dto.getAccessUrls());
        return entity;
    }

    public PrivilegeDTO map(Privilege entity) {
        return new PrivilegeDTO(entity.getAccessUrls(), entity.getCreated().getTime(), entity.getDescription(), entity.getLabel(), entity.getName());
    }
}
