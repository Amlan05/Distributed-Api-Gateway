package com.gateway.order_service.order.repository;

import com.gateway.order_service.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByOwnerSubjectOrderById(String ownerSubject);

    Optional<OrderEntity> findByIdAndOwnerSubject(Long id, String ownerSubject);
}
