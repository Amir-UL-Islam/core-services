package com.central.security.core.security.sod.repository;

import com.central.security.core.security.sod.model.entity.SodConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SodConstraintRepository extends JpaRepository<SodConstraint, Long> {

    /**
     * Check if a constraint already exists for this pair (order-independent).
     */
    @Query("SELECT COUNT(s) > 0 FROM SodConstraint s " +
           "WHERE (UPPER(s.roleNameA) = UPPER(:a) AND UPPER(s.roleNameB) = UPPER(:b)) " +
           "OR    (UPPER(s.roleNameA) = UPPER(:b) AND UPPER(s.roleNameB) = UPPER(:a))")
    boolean existsForPair(String a, String b);

    /** Load all active constraints for bulk checking during role assignment. */
    List<SodConstraint> findAll();

    /** Load only static SoD constraints (dynamic=false) for co-assignment enforcement. */
    List<SodConstraint> findAllByDynamicFalse();

    /** Load only dynamic SoD constraints for session-activation enforcement. */
    List<SodConstraint> findAllByDynamicTrue();
}

