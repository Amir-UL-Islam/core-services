package com.central.security.repositories;

import com.central.security.model.entites.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {
    @Query("SELECT p FROM Privilege p WHERE p.urlPattern = ?1 AND p.httpMethod = ?2")
    List<Privilege> findByUrlPatternAndHttpMethod(String urlPattern, String httpMethod);
}
