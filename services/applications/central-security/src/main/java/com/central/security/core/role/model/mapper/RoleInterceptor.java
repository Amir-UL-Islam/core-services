package com.central.security.core.role.model.mapper;

import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
import com.central.security.core.role.model.dto.RoleDTO;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.repository.RoleRepository;
import com.central.security.util.NotFoundException;
import com.problemfighter.pfspring.restapi.inter.CopyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleInterceptor implements CopyInterceptor<Role, RoleDTO, RoleDTO> {
    private final PrivilegeRepository privilegeRepository;
    private final RoleRepository roleRepository;

    @Override
    public void meAsSrc(RoleDTO source, Role destination) {
        final List<Privilege> privileges = privilegeRepository.findAllById(
                source.getPrivilege() == null ? List.of() : source.getPrivilege());

        if (privileges.size() != (source.getPrivilege() == null ? 0 : source.getPrivilege().size())) {
            throw new NotFoundException("one of privilege not found");
        }
        destination.setPrivilege(new HashSet<>(privileges));

        // Map parent role (hierarchy) — null clears the parent (makes this a root role)
        if (source.getParentId() != null) {
            Role parent = roleRepository.findById(source.getParentId())
                    .orElseThrow(() -> new NotFoundException("parent role not found"));
            destination.setParent(parent);
        } else {
            destination.setParent(null);
        }
    }

    @Override
    public void meAsDst(Role source, RoleDTO destination) {
        destination.setPrivilege(source.getPrivilege().stream()
                .map(Privilege::getId)
                .toList());
        destination.setParentId(source.getParent() != null ? source.getParent().getId() : null);
    }
}
