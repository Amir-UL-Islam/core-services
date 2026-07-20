package com.central.security.core.users;

import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.repository.RoleRepository;
import com.central.security.core.security.oauth.UserRoles;
import com.central.security.core.users.model.entity.Users;
import com.central.security.core.users.repository.UsersRepository;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Order(5)
@Slf4j
public class UserLoader implements ApplicationRunner {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserLoader(final UsersRepository usersRepository,
                      final RoleRepository roleRepository,
                      final PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        log.info("initializing default users");
        upsertUser(
                "superadmin",
                "superadmin@local.dev",
                "01700000002",
                "Super Admin",
                "superadmin123!",
                Set.of(UserRoles.SUPER_ADMIN)
        );
        upsertUser(
                "admin",
                "admin@local.dev",
                "01700000001",
                "Administrator",
                "admin123!",
                Set.of(UserRoles.ADMIN)
        );
        upsertUser(
                "user",
                "user@local.dev",
                "01700000000",
                "Standard User",
                "user123!",
                Set.of(UserRoles.USER)
        );
        upsertUser(
                "nicu_admin",
                "nicu_admin@local.dev",
                "01700000003",
                "NICU Admin",
                "nicuadmin123!",
                Set.of(UserRoles.NICU_ADMIN)
        );
        upsertUser(
                "hospital",
                "hospital@local.dev",
                "01700000004",
                "Hospital User",
                "hospital123!",
                Set.of(UserRoles.HOSPITAL)
        );
        upsertUser(
                "ambulance",
                "ambulance@local.dev",
                "01700000005",
                "Ambulance User",
                "ambulance123!",
                Set.of(UserRoles.AMBULANCE)
        );
        upsertUser(
                "moderator",
                "moderator@local.dev",
                "01700000006",
                "Moderator",
                "moderator123!",
                Set.of(UserRoles.MODERATOR)
        );
    }

    private void upsertUser(final String username,
                            final String email,
                            final String phone,
                            final String name,
                            final String rawPassword,
                            final Set<String> roleNames) {
        Users user = usersRepository.findByUsernameIgnoreCase(username);
        if (user == null) {
            user = new Users();
            user.setUsername(username);
        }

        user.setEmail(email);
        user.setPhone(phone);
        user.setGender("");
        user.setName(name);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setTwoFactorEnabled(false);
        user.setSmsMfaEnabled(false);
        user.setEmailMfaEnabled(false);
        user.setPreferredMfaFactor(null);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setAccountEnabled(true);

        final Set<Role> roles = new LinkedHashSet<>();
        roleNames.forEach(roleName -> {
            final Optional<Role> role = roleRepository.findByName(roleName);
            role.ifPresent(roles::add);
        });
        user.setRole(roles);

        if (user.getTokenVersion() == 0) {
            user.setTokenVersion(1);
        }

        usersRepository.save(user);
    }
}

