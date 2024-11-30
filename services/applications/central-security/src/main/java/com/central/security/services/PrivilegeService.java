package com.central.security.services;

import com.central.security.model.dtos.PrivilegeDTO;
import com.central.security.model.entites.Privilege;
import com.central.security.model.mappers.PrivilegeMapper;
import com.central.security.repositories.PrivilegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivilegeService {

    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeMapper privilegeMapper;

    public List<PrivilegeDTO> getAllPrivilege() {
        return privilegeRepository.findAll().stream()
                .map(privilegeMapper::map).collect(Collectors.toList());
    }

    public void save(PrivilegeDTO dto) {
        Privilege privilege = privilegeMapper.map(dto);
        privilegeRepository.save(privilege);
    }

    public Privilege findById(Long id) {
        return privilegeRepository.findById(id).orElse(null);
    }
}
