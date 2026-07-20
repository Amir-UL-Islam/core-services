package com.central.security.core.users.repository;

import java.util.List;

import com.central.security.core.users.model.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface UsersRepository extends JpaRepository<Users, Long> {

    @EntityGraph(attributePaths = {"role", "role.privilege"})
    Users findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"role", "role.privilege"})
    Users findByEmail(String email);

    Page<Users> findAllById(Long id, Pageable pageable);

    boolean existsByUsernameIgnoreCase(String username);

    List<Users> findAllByRoleId(Long id);

    /**
     * Find all users that have a specific role.
     * Used for token invalidation when role is modified/deleted.
     */
    @Query("SELECT u FROM Users u JOIN u.role r WHERE r.id = :roleId")
    List<Users> findUsersWithRole(Long roleId);

    @Query("SELECT u FROM Users u WHERE " +
            "(:filter IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :filter, '%'))) AND u.deleted = FALSE ")
    Page<Users> searchUsers(String filter, Pageable pageable);
    @Query("SELECT COUNT(u) FROM Users u JOIN u.role r WHERE r.name = :roleName AND u.deleted = FALSE")
    Long countUsersWithRoleName(@Param("roleName") String roleName);


    @Query("SELECT u FROM Users u JOIN u.role r WHERE " +
            "(:filter IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :filter, '%'))) AND " +
            "(:roleName IS NULL OR r.name = :roleName) AND " +
            "u.deleted = FALSE")
    Page<Users> searchUsers(
            @Param("filter") String filter,
            @Param("roleName") String roleName,
            Pageable pageable
    );

    @Query("SELECT u FROM Users u JOIN u.role r WHERE " +
            "(:filter IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :filter, '%'))) AND " +
            "(:roleName IS NULL OR r.name = :roleName)")
    Page<Users> searchWithDeletedUsers(
            @Param("filter") String filter,
            @Param("roleName") String roleName,
            Pageable pageable
    );

}
