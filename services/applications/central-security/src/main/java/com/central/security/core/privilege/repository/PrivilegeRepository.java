package com.central.security.core.privilege.repository;

import com.central.security.core.privilege.model.entity.Privilege;
import com.central.security.core.urls.model.entity.Url;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    boolean existsByNameIgnoreCase(String name);

    /**
     * Eagerly loads all privileges together with their associated URLs in a single
     * LEFT JOIN query. Used by {@link com.central.security.core.privilege.service.implmentation.PrivilegeService#hasPermission}
     * to avoid N+1 lazy loads on every ACL check. Result is cached by the service layer.
     */
    @EntityGraph(attributePaths = {"urls"})
    @Query("SELECT DISTINCT p FROM Privilege p LEFT JOIN FETCH p.urls")
    List<Privilege> findAllWithUrls();

    @EntityGraph(attributePaths = {"urls"})
    Optional<Privilege> findByNameIgnoreCase(String name);

    /**
     * Find privileges assigned to the given exact endpoint and method.
     * Uses exact matching on (endpoint, method) tuple.
     * Path variable matching is handled by the pattern matcher in service layer.
     *
     * @param endpoint The endpoint pattern (e.g., /api/users/{id})
     * @param httpMethod The HTTP method (GET, POST, etc.)
     * @return Privileges assigned to this endpoint+method combination
     */
    @EntityGraph(attributePaths = {"urls"})
    @Query("SELECT DISTINCT p FROM Privilege p JOIN p.urls u " +
           "WHERE u.endpoint = :endpoint AND LOWER(u.method) = LOWER(:httpMethod)")
    List<Privilege> findByExactEndpointAndMethod(String endpoint, String httpMethod);

    /**
     * Find privileges by exact endpoint, used during policy discovery.
     */
    @EntityGraph(attributePaths = {"urls"})
    @Query("SELECT DISTINCT p FROM Privilege p JOIN p.urls u WHERE u.endpoint = :endpoint")
    List<Privilege> findPrivilegesByEndpoint(String endpoint);

}
