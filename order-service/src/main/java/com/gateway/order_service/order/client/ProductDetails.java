package com.gateway.order_service.order.client;

import java.math.BigDecimal;

public record ProductDetails(
        Long id,
        String name,
        BigDecimal price
) {
}
