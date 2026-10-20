package com.gateway.product_service.product.exception;

public class ProductNotFoundException extends RuntimeException{

    public ProductNotFoundException(Long id){
        super("Product with id " + id + " not found");
    }
}
