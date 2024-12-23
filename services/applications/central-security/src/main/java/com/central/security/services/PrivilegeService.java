package com.central.security.services;

import com.central.security.model.dtos.PrivilegeDTO;
import com.central.security.model.entites.Privilege;
import com.central.security.model.mappers.PrivilegeMapper;
import com.central.security.repositories.PrivilegeRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
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

    public boolean hasPermission(Authentication authentication, HttpServletRequest request) {
        // Extract the current URL pattern and HTTP method
        String urlPattern = request.getRequestURI();

        String httpMethod = request.getMethod();

        // Find permissions for this URL and method
        List<Privilege> permissions = privilegeRepository.findByUrlPatternAndHttpMethod(urlPattern, httpMethod);

        // If no specific permissions, deny access
        if (permissions.isEmpty()) {
            return false;
        }

        // Get user authorities
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Check if user has required role
        return permissions.stream()
                .anyMatch(permission ->
                        authorities.stream()
                                .anyMatch(authority ->
                                        authority.getAuthority().equals(permission.getPermissions().name())
                                )
                );
    }


    public Privilege find(Long id) {
        return privilegeRepository.findById(id).orElse(null);
    }

    public Privilege save(Privilege permission) {
        return privilegeRepository.save(permission);
    }
}
