package com.central.security.core.privilege;

import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
import com.central.security.core.security.oauth.PermissionCodes;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@Order(1)
@Slf4j
public class PrivilegeLoader implements ApplicationRunner {

    private final PrivilegeRepository privilegeRepository;

    public PrivilegeLoader(final PrivilegeRepository privilegeRepository) {
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        log.info("initializing privileges");
        createIfMissing(PermissionCodes.ADMIN);
        createIfMissing(PermissionCodes.USER);

        createIfMissing(PermissionCodes.USER_READ);
        createIfMissing(PermissionCodes.USER_CREATE);
        createIfMissing(PermissionCodes.USER_UPDATE);
        createIfMissing(PermissionCodes.USER_DELETE);

        createIfMissing(PermissionCodes.ROLE_READ);
        createIfMissing(PermissionCodes.ROLE_CREATE);
        createIfMissing(PermissionCodes.ROLE_UPDATE);
        createIfMissing(PermissionCodes.ROLE_DELETE);

        createIfMissing(PermissionCodes.PRIVILEGE_READ);
        createIfMissing(PermissionCodes.PRIVILEGE_CREATE);
        createIfMissing(PermissionCodes.PRIVILEGE_UPDATE);
        createIfMissing(PermissionCodes.PRIVILEGE_DELETE);
        createIfMissing(PermissionCodes.PRIVILEGE_ASSIGN);

        createIfMissing(PermissionCodes.URL_READ);
        createIfMissing(PermissionCodes.URL_CREATE);
        createIfMissing(PermissionCodes.URL_UPDATE);
        createIfMissing(PermissionCodes.URL_DELETE);

        createIfMissing(PermissionCodes.MODULE_READ);
        createIfMissing(PermissionCodes.MODULE_MANAGE);

        createIfMissing(PermissionCodes.MATRIX_READ);
        createIfMissing(PermissionCodes.MATRIX_MANAGE);

        createIfMissing(PermissionCodes.POLICY_READ);
        createIfMissing(PermissionCodes.POLICY_MANAGE);

        createIfMissing(PermissionCodes.MFA_MANAGE);
        createIfMissing(PermissionCodes.MFA_CHALLENGE);
        createIfMissing(PermissionCodes.MFA_VERIFY);

        createIfMissing(PermissionCodes.AMBULANCE_READ);
        createIfMissing(PermissionCodes.AMBULANCE_CREATE);
        createIfMissing(PermissionCodes.AMBULANCE_UPDATE);
        createIfMissing(PermissionCodes.AMBULANCE_DELETE);

        // Dashboard domain
        createIfMissing(PermissionCodes.DASHBOARD_READ);

        // Hospital domain
        createIfMissing(PermissionCodes.HOSPITAL_READ);
        createIfMissing(PermissionCodes.HOSPITAL_CREATE);
        createIfMissing(PermissionCodes.HOSPITAL_UPDATE);
        createIfMissing(PermissionCodes.HOSPITAL_DELETE);
        createIfMissing(PermissionCodes.HOSPITAL_READ_OWN);
        createIfMissing(PermissionCodes.HOSPITAL_UPDATE_OWN);

        // Admission domain
        createIfMissing(PermissionCodes.ADMISSION_READ);
        createIfMissing(PermissionCodes.ADMISSION_CREATE);
        createIfMissing(PermissionCodes.ADMISSION_UPDATE);
        createIfMissing(PermissionCodes.ADMISSION_READ_OWN);

        // Admin registration
        createIfMissing(PermissionCodes.ADMIN_REGISTER_HOSPITAL);
        createIfMissing(PermissionCodes.ADMIN_REGISTER_AMBULANCE);
        createIfMissing(PermissionCodes.ADMIN_REGISTER_NICU_ADMIN);

        // Referral domain
        createIfMissing(PermissionCodes.REFERRAL_READ);
        createIfMissing(PermissionCodes.REFERRAL_CREATE);
        createIfMissing(PermissionCodes.REFERRAL_MANAGE);
        createIfMissing(PermissionCodes.REFERRAL_READ_OWN);

        // Scoped / parameterized permissions (RBAC3 §4)
        createIfMissing(PermissionCodes.AMBULANCE_READ_OWN);
        createIfMissing(PermissionCodes.AMBULANCE_CREATE_UNIT);
        createIfMissing(PermissionCodes.AMBULANCE_UPDATE_OWN);
        createIfMissing(PermissionCodes.USER_READ_OWN);
        createIfMissing(PermissionCodes.USER_UPDATE_OWN);

        // SoD constraint management
        createIfMissing(PermissionCodes.SOD_READ);
        createIfMissing(PermissionCodes.SOD_MANAGE);

        // Role dependency management
        createIfMissing(PermissionCodes.ROLE_DEPENDENCY_READ);
        createIfMissing(PermissionCodes.ROLE_DEPENDENCY_MANAGE);

        // Role hierarchy management
        createIfMissing(PermissionCodes.HIERARCHY_READ);
        createIfMissing(PermissionCodes.HIERARCHY_MANAGE);

        // Active-role context switching
        createIfMissing(PermissionCodes.CONTEXT_SWITCH);

        // Verification
        createIfMissing(PermissionCodes.VERIFICATION_PHONE_REQUEST);
        createIfMissing(PermissionCodes.VERIFICATION_PHONE_VERIFY);
        createIfMissing(PermissionCodes.VERIFICATION_EMAIL_REQUEST);
        createIfMissing(PermissionCodes.VERIFICATION_EMAIL_VERIFY);
    }

    private void createIfMissing(final String name) {
        if (privilegeRepository.existsByNameIgnoreCase(name)) {
            return;
        }
        final Privilege privilege = new Privilege();
        privilege.setName(name);
        privilegeRepository.saveAndFlush(privilege);
    }
}

