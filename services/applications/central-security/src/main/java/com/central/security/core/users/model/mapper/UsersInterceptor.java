package com.central.security.core.users.model.mapper;

import com.problemfighter.java.base.entity.BaseUserEntity;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.repository.RoleRepository;
import com.central.security.core.role.service.RoleDependencyService;
import com.central.security.core.security.sod.SodConstraintService;
import com.central.security.core.users.model.dto.UsersDTO;
import com.central.security.core.users.model.entity.Users;
import com.central.security.util.NotFoundException;
import com.problemfighter.pfspring.restapi.inter.CopyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UsersInterceptor implements CopyInterceptor<Users, UsersDTO, UsersDTO> {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SodConstraintService sodConstraintService;
    private final RoleDependencyService roleDependencyService;

    @Override
    public void meAsSrc(UsersDTO source, Users destination) {
        // Only handle custom logic:
        // 1. Password encoding (transformation)
        // 2. Role relationship (ID -> Entity)
        // Note: library auto-maps name, email, username, twoFactorEnabled, totpSecret

        // Password update priority:
        //   - UPDATE flow: newPassword (already verified by service layer via currentPassword check)
        //   - CREATE flow: password (plain password provided at creation time)
        // If neither is provided, keep the existing password unchanged.
        final String rawPassword = (source.getNewPassword() != null && !source.getNewPassword().trim().isEmpty())
                ? source.getNewPassword()
                : source.getPassword();

        if (rawPassword != null && !rawPassword.trim().isEmpty()) {
            destination.setPassword(passwordEncoder.encode(rawPassword));
        }

        // Handle role relationships
        final List<Role> roles = roleRepository.findAllById(
                source.getRole() == null ? List.of() : source.getRole());

        if (roles.size() != (source.getRole() == null ? 0 : source.getRole().size())) {
            throw new NotFoundException("one of role not found");
        }

        // RBAC3 SoD enforcement: reject assignment that would violate mutually-exclusive role constraints
        final List<String> newRoleNames = roles.stream().map(Role::getName).toList();
        sodConstraintService.assertNoPairConflict(newRoleNames);

        // Role dependency enforcement: every role's prerequisites must also be present
        roleDependencyService.assertSatisfied(new HashSet<>(roles));

        destination.setRole(new HashSet<>(roles));
    }

    @Override
    public void meAsDst(Users source, UsersDTO destination) {
        // Only handle role relationship (Entity -> ID list)
        // Note: id, name, email, username, twoFactorEnabled, totpSecret are auto-mapped by library
        // Note: password is NOT mapped (security - never send password to client)

        destination.setRole(source.getRole().stream()
                .map(BaseUserEntity::getId)
                .toList());
    }
}
