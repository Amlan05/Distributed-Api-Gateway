package com.gateway.order_service.order.model;

import java.math.BigDecimal;

public record Order(
        Long id,
        Long userId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
