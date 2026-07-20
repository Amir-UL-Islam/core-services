package com.central.security.core.role;

import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
import com.central.security.core.security.oauth.PermissionCodes;
import com.central.security.core.security.oauth.UserRoles;
import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.repository.RoleRepository;
import jakarta.transaction.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@Order(3)
@Slf4j
public class RoleLoader implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    public RoleLoader(final RoleRepository roleRepository,
                      final PrivilegeRepository privilegeRepository) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
    }

    /**
     * Resolve role from DB by name — assumes RoleLoader already ran.
     */
    private Role findRole(String name) {
        return roleRepository.findAll().stream()
                .filter(r -> name.equalsIgnoreCase(r.getName()))
                .findFirst().orElse(null);
    }

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        log.info("initializing roles");

        resetRolePrivilegeMatrixToDefaults();

        // Seed data-driven role hierarchy: SUPER_ADMIN > ADMIN > USER
        // Only set if parent is not already configured to avoid overwriting custom hierarchies.
        seedHierarchy();
    }

    /**
     * Re-applies the default role->privilege matrix used at program initialization.
     * Existing system roles are updated in place and custom roles are left untouched.
     */
    @Transactional
    public void resetRolePrivilegeMatrixToDefaults() {
        log.info("resetting role privilege matrix to default seed state");

        upsertRole(UserRoles.SUPER_ADMIN, "Super Administrator",
                resolveAllPrivileges());

        upsertRole(UserRoles.ADMIN, "Administrator",
                resolvePrivileges(
                        PermissionCodes.ADMIN,
                        PermissionCodes.USER,
                        PermissionCodes.USER_READ,
                        PermissionCodes.USER_CREATE,
                        PermissionCodes.USER_UPDATE,
                        PermissionCodes.USER_DELETE,
                        PermissionCodes.ROLE_READ,
                        PermissionCodes.PRIVILEGE_READ,
                        PermissionCodes.URL_READ,
                        PermissionCodes.MODULE_READ,
                        PermissionCodes.MATRIX_READ,
                        PermissionCodes.MATRIX_MANAGE,
                        PermissionCodes.POLICY_READ,
                        PermissionCodes.POLICY_MANAGE,
                        PermissionCodes.MFA_MANAGE,
                        PermissionCodes.MFA_CHALLENGE,
                        PermissionCodes.MFA_VERIFY,
                        PermissionCodes.SOD_READ,
                        PermissionCodes.ROLE_DEPENDENCY_READ,
                        PermissionCodes.ROLE_DEPENDENCY_MANAGE,
                        PermissionCodes.HIERARCHY_READ,
                        PermissionCodes.CONTEXT_SWITCH,
                        PermissionCodes.HOSPITAL_READ,
                        PermissionCodes.AMBULANCE_READ,
                        PermissionCodes.AMBULANCE_CREATE,
                        PermissionCodes.AMBULANCE_UPDATE,
                        PermissionCodes.DASHBOARD_READ
                ));

        upsertRole(UserRoles.USER, "Basic User",
                resolvePrivileges(
                        PermissionCodes.USER,
                        PermissionCodes.USER_CREATE,
                        PermissionCodes.USER_UPDATE,
                        PermissionCodes.USER_DELETE,
                        PermissionCodes.USER_READ,
                        PermissionCodes.USER_READ_OWN,
                        PermissionCodes.USER_UPDATE_OWN,
                        PermissionCodes.MATRIX_READ,
                        PermissionCodes.MFA_MANAGE,
                        PermissionCodes.MFA_CHALLENGE,
                        PermissionCodes.MFA_VERIFY,
                        PermissionCodes.VERIFICATION_PHONE_REQUEST,
                        PermissionCodes.VERIFICATION_PHONE_VERIFY,
                        PermissionCodes.VERIFICATION_EMAIL_REQUEST,
                        PermissionCodes.VERIFICATION_EMAIL_VERIFY,
                        PermissionCodes.CONTEXT_SWITCH,
                        PermissionCodes.HOSPITAL_READ,
                        PermissionCodes.AMBULANCE_READ,
                        PermissionCodes.AMBULANCE_READ_OWN,   // scoped: own records only
                        PermissionCodes.AMBULANCE_CREATE_UNIT // scoped: own unit only
                ));

        upsertRole(UserRoles.NICU_ADMIN, "NICU Administrator",
                resolvePrivileges(
                        PermissionCodes.ADMIN,
                        PermissionCodes.USER,
                        PermissionCodes.USER_CREATE,
                        PermissionCodes.USER_READ,
                        PermissionCodes.USER_UPDATE,
                        PermissionCodes.MATRIX_MANAGE,
                        PermissionCodes.CONTEXT_SWITCH,
                        PermissionCodes.HOSPITAL_READ,
                        PermissionCodes.HOSPITAL_CREATE,
                        PermissionCodes.HOSPITAL_UPDATE,
                        PermissionCodes.HOSPITAL_DELETE,
                        PermissionCodes.AMBULANCE_READ,
                        PermissionCodes.AMBULANCE_CREATE,
                        PermissionCodes.AMBULANCE_UPDATE,
                        PermissionCodes.AMBULANCE_DELETE,
                        PermissionCodes.ADMISSION_READ,
                        PermissionCodes.REFERRAL_READ,
                        PermissionCodes.ADMIN_REGISTER_HOSPITAL,
                        PermissionCodes.ADMIN_REGISTER_AMBULANCE,
                        PermissionCodes.DASHBOARD_READ
                ));

        upsertRole(UserRoles.HOSPITAL, "Hospital User",
                resolvePrivileges(
                        PermissionCodes.USER,
                        PermissionCodes.USER_CREATE,
                        PermissionCodes.USER_READ,
                        PermissionCodes.USER_UPDATE,
                        PermissionCodes.USER_READ_OWN,
                        PermissionCodes.USER_UPDATE_OWN,
                        PermissionCodes.CONTEXT_SWITCH,
                        PermissionCodes.HOSPITAL_READ,
                        PermissionCodes.HOSPITAL_CREATE,
                        PermissionCodes.HOSPITAL_UPDATE,
                        PermissionCodes.HOSPITAL_DELETE,
                        PermissionCodes.AMBULANCE_READ,
                        PermissionCodes.HOSPITAL_READ_OWN,
                        PermissionCodes.HOSPITAL_UPDATE_OWN,
                        PermissionCodes.ADMISSION_READ_OWN,
                        PermissionCodes.ADMISSION_READ,
                        PermissionCodes.ADMISSION_CREATE,
                        PermissionCodes.ADMISSION_UPDATE,
                        PermissionCodes.REFERRAL_READ,
                        PermissionCodes.REFERRAL_MANAGE,
                        PermissionCodes.REFERRAL_CREATE,
                        PermissionCodes.REFERRAL_READ_OWN,
                        PermissionCodes.DASHBOARD_READ
                ));

        upsertRole(UserRoles.AMBULANCE, "Ambulance User",
                resolvePrivileges(
                        PermissionCodes.USER,
                        PermissionCodes.USER_READ_OWN,
                        PermissionCodes.USER_UPDATE_OWN,
                        PermissionCodes.CONTEXT_SWITCH,
                        PermissionCodes.HOSPITAL_READ,
                        PermissionCodes.AMBULANCE_READ,
                        PermissionCodes.AMBULANCE_CREATE,
                        PermissionCodes.AMBULANCE_UPDATE,
                        PermissionCodes.AMBULANCE_DELETE,
                        PermissionCodes.AMBULANCE_READ_OWN,
                        PermissionCodes.AMBULANCE_UPDATE_OWN
                ));

        upsertRole(UserRoles.MODERATOR, "Moderator",
                resolvePrivileges(
                        PermissionCodes.USER,
                        PermissionCodes.CONTEXT_SWITCH,
                        PermissionCodes.HOSPITAL_READ,
                        PermissionCodes.AMBULANCE_READ,
                        PermissionCodes.ADMISSION_READ,
                        PermissionCodes.REFERRAL_READ,
                        PermissionCodes.DASHBOARD_READ
                ));
    }

    /**
     * Seeds the role hierarchy using "parent = the junior role this role dominates / inherits from":
     * SUPER_ADMIN.parent = ADMIN → SUPER_ADMIN is senior to ADMIN (inherits ADMIN's privileges)
     * ADMIN.parent       = USER  → ADMIN is senior to USER (inherits USER's privileges)
     * Only seeds if not already configured to avoid overwriting custom hierarchies.
     */
    private void seedHierarchy() {
        Role superAdmin = findRole(UserRoles.SUPER_ADMIN);
        Role admin = findRole(UserRoles.ADMIN);
        Role user = findRole(UserRoles.USER);
        Role nicuAdmin = findRole(UserRoles.NICU_ADMIN);
        Role hospital = findRole(UserRoles.HOSPITAL);
        Role ambulance = findRole(UserRoles.AMBULANCE);
        Role moderator = findRole(UserRoles.MODERATOR);

        if (superAdmin != null && admin != null && superAdmin.getParent() == null) {
            superAdmin.setParent(admin);
            roleRepository.save(superAdmin);
            log.info("Seeded hierarchy: {} dominates {}", UserRoles.SUPER_ADMIN, UserRoles.ADMIN);
        }
        if (admin != null && user != null && admin.getParent() == null) {
            admin.setParent(user);
            roleRepository.save(admin);
            log.info("Seeded hierarchy: {} dominates {}", UserRoles.ADMIN, UserRoles.USER);
        }
        if (nicuAdmin != null && user != null && nicuAdmin.getParent() == null) {
            nicuAdmin.setParent(user);
            roleRepository.save(nicuAdmin);
            log.info("Seeded hierarchy: {} dominates {}", UserRoles.NICU_ADMIN, UserRoles.USER);
        }
        if (hospital != null && user != null && hospital.getParent() == null) {
            hospital.setParent(user);
            roleRepository.save(hospital);
            log.info("Seeded hierarchy: {} dominates {}", UserRoles.HOSPITAL, UserRoles.USER);
        }
        if (ambulance != null && user != null && ambulance.getParent() == null) {
            ambulance.setParent(user);
            roleRepository.save(ambulance);
            log.info("Seeded hierarchy: {} dominates {}", UserRoles.AMBULANCE, UserRoles.USER);
        }
        if (moderator != null && user != null && moderator.getParent() == null) {
            moderator.setParent(user);
            roleRepository.save(moderator);
            log.info("Seeded hierarchy: {} dominates {}", UserRoles.MODERATOR, UserRoles.USER);
        }
    }

    private Set<Privilege> resolveAllPrivileges() {
        return new LinkedHashSet<>(privilegeRepository.findAll());
    }

    private Set<Privilege> resolvePrivileges(final String... privilegeNames) {
        final Set<Privilege> privileges = new LinkedHashSet<>();
        Arrays.stream(privilegeNames)
                .map(name -> privilegeRepository.findByNameIgnoreCase(name).orElse(null))
                .filter(Objects::nonNull)
                .forEach(privileges::add);
        return privileges;
    }

    private void upsertRole(final String roleName, final String description, final Set<Privilege> privileges) {
        Role role = roleRepository.findAll().stream()
                .filter(r -> roleName.equalsIgnoreCase(r.getName()))
                .findFirst()
                .orElseGet(Role::new);
        role.setName(roleName);
        role.setDescription(description);
        role.setPrivilege(privileges);
        roleRepository.save(role);
    }

}
