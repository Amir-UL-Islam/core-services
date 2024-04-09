package com.orders.repository;

import com.orders.model.entites.OrderStack;
import com.orders.model.entites.PlacedItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends DomainSpecificRepositoryDefinition<OrderStack, Long> {
}
