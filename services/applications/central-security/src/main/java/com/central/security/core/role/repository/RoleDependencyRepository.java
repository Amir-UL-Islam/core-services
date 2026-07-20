package com.central.security.core.role.repository;

import com.central.security.core.role.model.entity.Role;
import com.central.security.core.role.model.entity.RoleDependency;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDependencyRepository extends JpaRepository<RoleDependency, Long> {

    /** All dependencies where the dependent role is one of the given role IDs. */
    List<RoleDependency> findAllByDependentRoleIdIn(Collection<Long> roleIds);

    boolean existsByDependentRoleAndRequiredRole(Role dependentRole, Role requiredRole);
}
