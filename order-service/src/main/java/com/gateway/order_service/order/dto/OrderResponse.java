package com.gateway.order_service.order.dto;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long userId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
