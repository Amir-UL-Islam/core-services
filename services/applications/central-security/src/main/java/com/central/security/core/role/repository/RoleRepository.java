package com.central.security.core.role.repository;

import java.util.List;
import java.util.Optional;

import com.central.security.core.role.model.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Page<Role> findAllById(Long id, Pageable pageable);

    List<Role> findAllByPrivilegeId(Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByDescriptionIgnoreCase(String description);

    @Query("SELECT r FROM Role r " +
            "WHERE r.deleted = false " +
            "AND (:query IS NULL " +
            " OR LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            " OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))" +
            ")")
    Page<Role> search(String query, Pageable pageable);
}
