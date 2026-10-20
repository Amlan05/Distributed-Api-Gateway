package com.gateway.order_service.order.repository;

import com.gateway.order_service.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}
