package com.orders.repository;

import com.orders.model.entites.OrderStack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderStack, Long> {
}
