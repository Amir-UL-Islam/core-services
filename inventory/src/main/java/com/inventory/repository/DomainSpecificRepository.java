package com.inventory.repository;

import com.inventory.model.Inventory;
import org.springframework.data.repository.RepositoryDefinition;

import java.util.Set;

@RepositoryDefinition(domainClass = Inventory.class, idClass = Long.class)
public interface DomainSpecificRepository {
    // This will return true if any of the skuCode is present in the database
    boolean existsBySkuCodeIn(Set<String> skuCode);

}
