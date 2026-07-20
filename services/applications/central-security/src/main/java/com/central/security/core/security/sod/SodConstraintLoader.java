package com.central.security.core.security.sod;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds built-in Separation-of-Duties constraints on startup.
 *
 * These constraints represent roles that are inherently incompatible in the
 * NICU application domain.  They run at {@code @Order(10)} — after all roles
 * and privileges are initialized — and are idempotent (safe to re-run).
 */
@Component
@Order(10)
@Slf4j
public class SodConstraintLoader implements ApplicationRunner {

    private final SodConstraintService sodConstraintService;

    public SodConstraintLoader(final SodConstraintService sodConstraintService) {
        this.sodConstraintService = sodConstraintService;
    }

    @Override
    public void run(final ApplicationArguments args) {
        log.info("Seeding SoD constraints");

        /*
         * A regular USER cannot simultaneously be a SUPER_ADMIN.
         * Rationale: prevents privilege escalation via dual assignment.
         */
        sodConstraintService.registerConstraint(
                "USER", "SUPER_ADMIN",
                "A regular user cannot also hold super-administrator privileges");

        /*
         * ADMIN and USER are not mutually exclusive by design (role hierarchy
         * already handles inheritance), but the intent is to keep them separate
         * to preserve the principle of least privilege at the audit boundary.
         * Comment out / remove this constraint if your application legitimately
         * needs a user to act in both capacities.
         */
        // sodConstraintService.registerConstraint("ADMIN", "USER", "...");

        log.info("SoD constraints seeded");
    }
}

