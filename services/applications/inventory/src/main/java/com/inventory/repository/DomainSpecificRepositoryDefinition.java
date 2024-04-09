package com.inventory.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * This is a custom repository definition which extends the Spring Data Repository interface.
 * It provides a way to define custom methods that are not present in the Spring Data Repository interface.
 * Here, we have defined two methods findById and save.
 * These methods are routed into the base repository implementation of the store(Database) of your choice provided by Spring Data (for example, if you use JPA, the implementation is SimpleJpaRepository),
 * because they match the method signatures in CrudRepository. So any Interface extended this interface can now save, find individual by ID, and trigger a query to find by fields.
 *
 * @param <T>
 * @param <ID>
 */
@NoRepositoryBean
public interface DomainSpecificRepositoryDefinition<T, ID> extends Repository<T, ID> {
    @Query("SELECT t FROM #{#entityName} t WHERE t.id = ?1")
    Optional<T> findById(ID id);

    <S extends T> S save(S entity);
}
