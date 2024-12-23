package com.central.security.services;

import com.central.security.model.dtos.RoleDTO;
import com.central.security.model.entites.Role;
import com.central.security.model.enums.Roles;
import com.central.security.model.mappers.RoleMapper;
import com.central.security.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public Role findByName(Roles name) {
        return roleRepository.findByName(name);
    }

    public void save(RoleDTO dto) {
        Role role = roleMapper.map(dto);
        roleRepository.save(role);
    }

}
