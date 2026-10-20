package com.gateway.order_service.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest (

    @NotNull(message = "User id is required")
    @Positive(message = "User id must be positive")
    Long userId,

    @NotNull(message = "Product is is required")
    @Positive(message = "Product id must be positive")
    Long productId,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Max(value = 10000, message = "Quantity must not exceed 10000")
    Integer quantity
){
}
