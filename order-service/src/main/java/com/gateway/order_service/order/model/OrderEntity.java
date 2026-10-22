package com.gateway.order_service.order.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(
            name = "unit_price",
            nullable = false,
            updatable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
            name = "total_price",
            nullable = false,
            updatable = false,
            precision = 16,
            scale = 2
    )
    private BigDecimal totalPrice;

    // Legacy orders have no owner and are visible only to administrators.
    @Column(name = "owner_subject", length = 100, updatable = false)
    private String ownerSubject;

    protected OrderEntity() {

    }

     public OrderEntity(
            Long userId,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {
        this(userId, productId, quantity, unitPrice, null);
    }

    public OrderEntity(Long userId, Long productId, Integer quantity,
                       BigDecimal unitPrice, String ownerSubject) {
        this.ownerSubject = ownerSubject;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    public Long getUserId(){
        return userId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
}
