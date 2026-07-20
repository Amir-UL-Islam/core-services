package com.central.security.core.urls;

import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.privilege.repository.PrivilegeRepository;
import com.central.security.core.security.oauth.PermissionCodes;
import com.central.security.core.urls.model.EndpointPatternMatcher;
import com.central.security.core.urls.model.entity.Url;
import com.central.security.core.urls.repository.UrlsRepository;
import jakarta.transaction.Transactional;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@Order(2)
@Slf4j
public class UrlsLoader implements ApplicationRunner {

    private final UrlsRepository urlsRepository;
    private final PrivilegeRepository privilegeRepository;

    public UrlsLoader(final UrlsRepository urlsRepository,
                      final PrivilegeRepository privilegeRepository) {
        this.urlsRepository = urlsRepository;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        log.info("seeding URL-privilege mappings (idempotent)");
        seedMatrixUrls();
    }

    private void seedMatrixUrls() {
        // Authentication & Token Management
        grant(PermissionCodes.USER,
                entry("/api/v1/authenticate", methods("POST")),
                entry("/api/v1/oauth/token", methods("POST")),
                entry("/api/v1/refresh-token", methods("POST")),
                entry("/api/v1/authenticateGoogle", methods("POST")));

        // Public Registration (no auth required)
        grant(PermissionCodes.USER,
                entry("/api/v1/register/init", methods("POST")),
                entry("/api/v1/register/verify", methods("POST")));

        // Shared basic read APIs available to all roles via USER privilege.
        grant(PermissionCodes.USER,
                entry("/api/v1/report/hospital/bed", methods("GET")),
                entry("/api/v1/addresses", methods("PATCH")),
                entry("/api/v1/addresses", methods("GET")),
                entry("/api/v1/addresses", methods("POST")),
                entry("/api/v1/addresses/{id}", methods("GET")),
                entry("/api/v1/users/{userId}/addresses", methods("GET")));

        // User Permissions & Context
        grant(PermissionCodes.MATRIX_READ,
                entry("/api/v1/me/permissions", methods("GET")));
        grant(PermissionCodes.CONTEXT_SWITCH,
                entry("/api/v1/me/switch-context", methods("POST")));

        grant(PermissionCodes.POLICY_READ,
                entry("/api/v1/security/policy", methods("GET")));
        grant(PermissionCodes.POLICY_MANAGE,
                entry("/api/v1/security/policy", methods("PATCH")));

        grant(PermissionCodes.MFA_MANAGE,
                entry("/api/v1/mfa/settings", methods("GET", "PUT")));

        // Verification endpoints - added to USER basic privileges
        grant(PermissionCodes.VERIFICATION_PHONE_REQUEST,
                entry("/api/v1/request/phone-verification", methods("POST")));
        grant(PermissionCodes.VERIFICATION_PHONE_VERIFY,
                entry("/api/v1/verify/phone", methods("PATCH")));
        grant(PermissionCodes.VERIFICATION_EMAIL_REQUEST,
                entry("/api/v1/request/email-verification", methods("POST")));
        grant(PermissionCodes.VERIFICATION_EMAIL_VERIFY,
                entry("/api/v1/verify/email", methods("PATCH")));

        grant(PermissionCodes.USER_READ,
                entry("/api/v1/user", methods("GET")),
                entry("/api/v1/user/{id}", methods("GET")));
        grant(PermissionCodes.USER_CREATE,
                entry("/api/v1/user", methods("POST")));
        grant(PermissionCodes.USER_UPDATE,
                entry("/api/v1/user", methods("PATCH")),
                entry("/api/v1/user/logout-all-devices", methods("POST")));
        grant(PermissionCodes.USER_DELETE,
                entry("/api/v1/user/{id}", methods("DELETE")));

        grant(PermissionCodes.ROLE_READ,
                entry("/api/v1/roles", methods("GET")),
                entry("/api/v1/roles/{id}", methods("GET")),
                entry("/api/v1/user/roleValues", methods("GET")));
        grant(PermissionCodes.ROLE_CREATE,
                entry("/api/v1/roles", methods("POST")));
        grant(PermissionCodes.ROLE_UPDATE,
                entry("/api/v1/roles/{id}", methods("PUT")));
        grant(PermissionCodes.ROLE_DELETE,
                entry("/api/v1/roles/{id}", methods("DELETE")));

        grant(PermissionCodes.PRIVILEGE_READ,
                entry("/api/v1/privileges", methods("GET")),
                entry("/api/v1/privileges/{id}", methods("GET")),
                entry("/api/v1/url/privilegeValues", methods("GET")),
                entry("/api/v1/roles/privilegeValues", methods("GET")));
        grant(PermissionCodes.PRIVILEGE_CREATE,
                entry("/api/v1/privileges", methods("POST")));
        grant(PermissionCodes.PRIVILEGE_UPDATE,
                entry("/api/v1/privileges", methods("PATCH")));
        grant(PermissionCodes.PRIVILEGE_DELETE,
                entry("/api/v1/privileges/{id}", methods("DELETE")));
        grant(PermissionCodes.PRIVILEGE_ASSIGN,
                entry("/api/v1/privileges/assign/url", methods("PATCH")),
                entry("/api/v1/privileges/remove/assess/url", methods("PATCH")),
                entry("/api/v1/privileges/remove/assign/url", methods("PATCH")));

        grant(PermissionCodes.URL_READ,
                entry("/api/v1/url", methods("GET")),
                entry("/api/v1/url/{id}", methods("GET")),
                entry("/api/v1/url/privilege/{id}", methods("GET")));
        grant(PermissionCodes.URL_CREATE,
                entry("/api/v1/url", methods("POST")));
        grant(PermissionCodes.URL_UPDATE,
                entry("/api/v1/url", methods("PUT")),
                entry("/api/v1/url/{id}", methods("PUT")));
        grant(PermissionCodes.URL_DELETE,
                entry("/api/v1/url/{id}", methods("DELETE")));

        grant(PermissionCodes.MODULE_READ,
                entry("/api/v1/modules", methods("GET")),
                entry("/api/v1/modules/{moduleId}", methods("GET")),
                entry("/api/v1/modules/active", methods("GET")));
        grant(PermissionCodes.MODULE_MANAGE,
                entry("/api/v1/modules/{moduleId}/enable", methods("POST")),
                entry("/api/v1/modules/{moduleId}/disable", methods("POST")),
                entry("/api/v1/modules/refresh", methods("POST")));

        grant(PermissionCodes.HOSPITAL_READ,
                entry("/api/v1/hospitals", methods("GET")),
                entry("/api/v1/hospitals/{id}", methods("GET")),
                entry("/api/v1/hospitals/owner/{ownerId}", methods("GET")),
                entry("/api/v1/hospitals/available", methods("GET")),
                entry("/api/v1/hospitals/nicu/bed/{uuid}", methods("GET")),
                entry("/api/v1/hospitals/beds/{uuid}/qr", methods("GET"))
        );
        grant(PermissionCodes.HOSPITAL_CREATE,
                entry("/api/v1/hospitals", methods("POST")),
                entry("/api/v1/hospitals/{hospitalId}/beds", methods("POST"))
        );
        grant(PermissionCodes.HOSPITAL_UPDATE,
                entry("/api/v1/hospitals", methods("PATCH")),
                entry("/api/v1/hospitals/beds/reserve", methods("PATCH")),
                entry("/api/v1/hospitals/beds/reserve/cancel/{nicuBedId}", methods("PATCH"))
        );
        grant(PermissionCodes.HOSPITAL_DELETE,
                entry("/api/v1/hospitals/{id}", methods("DELETE")),
                entry("/api/v1/hospitals/beds/{bedId}", methods("DELETE")));

        grant(PermissionCodes.AMBULANCE_READ,
                entry("/api/v1/ambulance/agency", methods("GET")),
                entry("/api/v1/ambulance/agency/{id}", methods("GET")),
                entry("/api/v1/ambulance/agency/owner/{ownerId}", methods("GET")),
                entry("/api/v1/ambulances/vehicles", methods("GET")),
                entry("/api/v1/ambulances/vehicles/{vehicleId}", methods("GET")));
        grant(PermissionCodes.AMBULANCE_CREATE,
                entry("/api/v1/ambulance/agency", methods("POST")),
                entry("/api/v1/ambulances/vehicles", methods("POST")));
        grant(PermissionCodes.AMBULANCE_UPDATE,
                entry("/api/v1/ambulance/agency", methods("PATCH")),
                entry("/api/v1/ambulances/vehicles", methods("PATCH")));
        grant(PermissionCodes.AMBULANCE_DELETE,
                entry("/api/v1/ambulance/agency/{id}", methods("DELETE")),
                entry("/api/v1/ambulances/vehicles/{vehicleId}", methods("DELETE")));

        // Keep legacy compatibility for endpoints that rely on generic authenticated user access.
        grant(PermissionCodes.USER,
                entry("/api/v1/2fa/setup", methods("POST")),
                entry("/api/v1/2fa/activate", methods("POST")),
                entry("/api/v1/2fa/disable", methods("POST")));

        // SoD constraint management
        grant(PermissionCodes.SOD_READ,
                entry("/api/v1/security/sod", methods("GET")));
        grant(PermissionCodes.SOD_MANAGE,
                entry("/api/v1/security/sod", methods("POST")),
                entry("/api/v1/security/sod/{id}", methods("DELETE")));

        // Role hierarchy management
        grant(PermissionCodes.HIERARCHY_READ,
                entry("/api/v1/roles/hierarchy", methods("GET")));
        grant(PermissionCodes.HIERARCHY_MANAGE,
                entry("/api/v1/roles/{id}/parent", methods("PUT")));

        // Role dependency management
        grant(PermissionCodes.ROLE_DEPENDENCY_READ,
                entry("/api/v1/role-dependencies", methods("GET")));
        grant(PermissionCodes.ROLE_DEPENDENCY_MANAGE,
                entry("/api/v1/role-dependencies", methods("POST")),
                entry("/api/v1/role-dependencies/{id}", methods("DELETE")));

        // Active-role context switching
        grant(PermissionCodes.CONTEXT_SWITCH,
                entry("/api/v1/me/switch-context", methods("POST")));

        // Policy drift management
        grant(PermissionCodes.MATRIX_MANAGE,
                entry("/api/v1/security/policy/drift", methods("GET")));

        // Full permission matrix (admin view + batch update)
        grant(PermissionCodes.MATRIX_READ,
                entry("/api/v1/security/permission-matrix", methods("GET")),
                entry("/api/v1/security/permission-matrix/grouped", methods("GET")));
        grant(PermissionCodes.MATRIX_MANAGE,
                entry("/api/v1/security/permission-matrix", methods("PATCH")),
                entry("/api/v1/security/permission-matrix/reset", methods("POST")),
                entry("/api/v1/security/permission-matrix/grouped", methods("PATCH")));

        // Admission management
        grant(PermissionCodes.ADMISSION_CREATE,
                entry("/api/v1/admissions", methods("POST")));
        grant(PermissionCodes.ADMISSION_READ,
                entry("/api/v1/admissions", methods("GET")),
                entry("/api/v1/admissions/{id}", methods("GET")));
        grant(PermissionCodes.ADMISSION_UPDATE,
                entry("/api/v1/admissions/{admissionId}/discharge", methods("POST")),
                entry("/api/v1/admissions/discharge/bed/{bedId}", methods("POST")),
                entry("/api/v1/admissions/discharge/bed/{uuid}", methods("POST"))
        );

        // Referral management
        grant(PermissionCodes.REFERRAL_CREATE,
                entry("/api/v1/referrals", methods("POST")));
        grant(PermissionCodes.REFERRAL_READ,
                entry("/api/v1/referrals", methods("GET")),
                entry("/api/v1/referrals/{id}", methods("GET")));
        grant(PermissionCodes.REFERRAL_MANAGE,
                entry("/api/v1/update/referrals/status/{id}", methods("POST")));
        grant(PermissionCodes.REFERRAL_READ_OWN,
                entry("/api/v1/referrals/outgoing/{hospitalId}", methods("GET")),
                entry("/api/v1/referrals/incoming/{hospitalId}", methods("GET")));

        // Admin registration (privileged)
        grant(PermissionCodes.ADMIN_REGISTER_NICU_ADMIN,
                entry("/api/v1/admin/nicu-admin/register", methods("POST")));
        grant(PermissionCodes.ADMIN_REGISTER_HOSPITAL,
                entry("/api/v1/admin/hospital/register", methods("POST")));
        grant(PermissionCodes.ADMIN_REGISTER_AMBULANCE,
                entry("/api/v1/admin/ambulance/register", methods("POST")));

        // Dashboard & reports
        grant(PermissionCodes.DASHBOARD_READ,
                entry("/api/v1/dashboard/summary", methods("GET")),
                entry("/api/v1/dashboard/nationwide/nicu/bed/summary", methods("GET")),
                entry("/api/v1/dashboard/monitor/summary", methods("GET")),
                entry("/api/v1/dashboard/referral/success/rate", methods("GET")),
                entry("/api/v1/dashboard/nicu/bed/status/division/wise", methods("GET")),
                entry("/api/v1/dashboard/hospital/referral/count/{hospitalId}", methods("GET")),
                entry("/api/v1/dashboard/ambulance/vehicle/stats/{ambulanceId}", methods("GET")),
                entry("/api/v1/dashboard/nicu/bed/summary/{hospitalId}", methods("GET")),
                entry("/api/v1/download/report/hospital/bed", methods("GET")));

        // Patient management
        grant(PermissionCodes.ADMISSION_READ,
                entry("/api/v1/patients", methods("GET")),
                entry("/api/v1/patients/{id}", methods("GET")));
    }

    @SafeVarargs
    private void grant(final String privilegeName, final Map.Entry<String, String[]>... endpoints) {
        final Privilege privilege = privilegeRepository.findByNameIgnoreCase(privilegeName).orElse(null);
        if (privilege == null) {
            log.warn("privilege {} missing, skipping url mapping", privilegeName);
            return;
        }
        for (final Map.Entry<String, String[]> endpoint : endpoints) {
            addUrls(endpoint.getKey(), endpoint.getValue(), privilege);
        }
    }

    private void addUrls(final String endpoint, final String[] methods, final Privilege privilege) {
        // Validate API design rules — fails startup if violated
        new EndpointPatternMatcher(endpoint).validateApiDesignRules();

        for (final String method : methods) {
            Url url = urlsRepository.findByEndpointIgnoreCaseAndMethodIgnoreCase(endpoint, method)
                    .orElse(null);

            if (url == null) {
                // URL doesn't exist yet — create it
                url = new Url();
                url.setEndpoint(endpoint);
                url.setMethod(method);
                urlsRepository.saveAndFlush(url);
            }

            // Ensure the privilege association exists (idempotent)
            if (!privilege.getUrls().contains(url)) {
                privilege.getUrls().add(url);
                url.getPrivileges().add(privilege);
                privilegeRepository.saveAndFlush(privilege);
            }
        }
    }

    private static String[] methods(final String... methods) {
        return methods;
    }

    private static Map.Entry<String, String[]> entry(final String key, final String[] value) {
        return Map.entry(key, value);
    }
}

