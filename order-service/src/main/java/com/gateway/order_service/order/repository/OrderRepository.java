package com.gateway.order_service.order.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.gateway.order_service.order.model.Order;
import com.gateway.order_service.order.model.OrderEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public List<Order> findAll() {
        return jpaRepository.findAll(Sort.by("id"))
                .stream()
                .map(this::toOrder)
                .toList();
    }

    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::toOrder);
    }

    @Transactional
    public Order create(
            Long userId,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice) {

        OrderEntity entity = new OrderEntity(
                userId,
                productId,
                quantity,
                unitPrice
        );

        return toOrder(jpaRepository.save(entity));
    }

    public List<Order> findAllOwned(String subject) {
        return jpaRepository.findAllByOwnerSubjectOrderById(subject).stream().map(this::toOrder).toList();
    }

    public Optional<Order> findOwnedById(Long id, String subject) {
        return jpaRepository.findByIdAndOwnerSubject(id, subject).map(this::toOrder);
    }

    @Transactional
    public Order createOwned(Long userId, Long productId, Integer quantity,
                             BigDecimal unitPrice, String subject) {
        return toOrder(jpaRepository.save(new OrderEntity(userId, productId, quantity, unitPrice, subject)));
    }

    private Order toOrder(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getUserId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getTotalPrice()
        );
    }
}